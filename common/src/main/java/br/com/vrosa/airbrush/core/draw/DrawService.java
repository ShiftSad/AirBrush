package br.com.vrosa.airbrush.core.draw;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.history.Change;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.ink.InkType;
import br.com.vrosa.airbrush.core.render.Curve;
import br.com.vrosa.airbrush.core.render.SegmentRenderer;
import br.com.vrosa.airbrush.core.ui.ActionBars;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public final class DrawService {

    private static final double SPACING_PER_WIDTH = 2.0;
    private static final double MIN_PARTIAL = 1.0e-3;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private record FadingSegment(@NotNull SegmentHandle handle, @NotNull Transform shape, long expireAt) {}

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Map<UUID, Integer> pencil = new HashMap<>();
    private final List<FadingSegment> fading = new ArrayList<>();

    private final Raycaster raycaster;
    private final History history;
    private final AirBrushConfig config;
    private final SegmentRenderer renderer;
    private final ActionBars bars;

    public DrawService(@NotNull Platform platform, @NotNull Raycaster raycaster,
                       @NotNull History history, @NotNull AirBrushConfig config, @NotNull ActionBars bars) {
        this.raycaster = raycaster;
        this.history = history;
        this.config = config;
        this.renderer = new SegmentRenderer(platform, config);
        this.bars = bars;
    }

    public boolean isActive(@NotNull WPlayer player) {
        return sessions.containsKey(player.uuid());
    }

    public boolean isHolding(@NotNull WPlayer player) {
        final var tool = player.heldTool();
        return tool == ToolType.PENCIL || tool == ToolType.QUILL;
    }

    public void setColor(@NotNull WPlayer player, int rgb) {
        pencil.put(player.uuid(), rgb & 0xFFFFFF);
    }

    public int colorOf(@NotNull WPlayer player) {
        if (player.heldTool() == ToolType.QUILL) {
            final var inkColor = player.heldToolInkColor();
            return inkColor != null ? inkColor : InkType.COMMON.waterRgb();
        }
        return pencil.getOrDefault(player.uuid(), DEFAULT_COLOR);
    }

    public double radiusOf(@NotNull WPlayer player) {
        final var saved = player.heldToolRadius();
        final double radius = saved != null ? saved : config.pencilDefaultRadius();
        return Math.clamp(radius, minRadius(player), maxRadius(player));
    }

    public void changeRadius(@NotNull WPlayer player, int direction) {
        final double current = radiusOf(player);
        final double next = Math.clamp(current + direction * config.pencilRadiusStep(),
                minRadius(player), maxRadius(player));
        player.setHeldToolRadius(next);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.5f, next > current ? 1.8f : 1.0f);
        bars.status(player, actionBar(player, next));
    }

    public void tick(@NotNull WPlayer player) {
        final var session = sessions.get(player.uuid());
        if (session != null && !isHolding(player)) {
            confirmActive(player);
            return;
        }

        if (isHolding(player)) bars.status(player, actionBar(player, radiusOf(player)));
        if (session == null) return;

        final var pointer = raycaster.current(player);
        if (session.mode == DrawMode.STRAIGHT) tickStraight(session, pointer);
        else tickFreehand(player, session, pointer);
    }

    public void confirmActive(@NotNull WPlayer player) {
        final var s = sessions.get(player.uuid());
        if (s == null) return;
        if (s.mode == DrawMode.STRAIGHT) finishStraight(player, s);
        else finishFreehand(player, s, null);
    }

    /** Shrinks and removes expired strokes of non-permanent inks. */
    public void tickSegments() {
        if (fading.isEmpty()) return;
        final long now = System.currentTimeMillis();
        final long shrinkMillis = config.markerFadeSeconds() * 1000L;

        final var iterator = fading.iterator();
        while (iterator.hasNext()) {
            final var segment = iterator.next();
            if (!segment.handle().isValid()) {
                iterator.remove();
                continue;
            }

            final long remaining = segment.expireAt() - now;
            if (remaining <= 0) {
                segment.handle().remove();
                iterator.remove();
                continue;
            }
            if (remaining >= shrinkMillis) continue;

            final float factor = (float) remaining / shrinkMillis;
            final var shape = segment.shape();
            final var scale = new Vector3f(shape.scale());
            scale.x *= factor;
            scale.y *= factor;
            segment.handle().setTransform(new Transform(shape.translation(), shape.leftRotation(),
                    scale, shape.rightRotation()));
        }
    }

    private @NotNull Component actionBar(@NotNull WPlayer player, double radius) {
        final var locale = player.locale();
        final var tool = player.heldTool();
        final var separator = Component.text("  •  ", NamedTextColor.DARK_GRAY);
        return Component.text(Messages.toolName(locale, tool == null ? ToolType.PENCIL : tool), NamedTextColor.GRAY)
                .append(separator)
                .append(Component.text(Messages.get(locale, Messages.Key.COLOR), TextColor.color(colorOf(player))))
                .append(separator)
                .append(Component.text(Messages.get(locale, Messages.Key.THICKNESS) + " ", NamedTextColor.GRAY))
                .append(Component.text(String.format(Locale.ROOT, "%.2f", radius), NamedTextColor.WHITE));
    }

    private void tickStraight(@NotNull DrawSession s, @Nullable Pose pointer) {
        if (s.rubberband == null || !s.rubberband.isValid()) return;
        if (pointer == null) {
            renderer.hide(s.rubberband);
            return;
        }
        if (!s.anchor.sameWorld(pointer)) return;
        renderer.orient(s.rubberband, s.anchor, pointer.position(), s.width);
    }

    private void tickFreehand(@NotNull WPlayer player, @NotNull DrawSession s, @Nullable Pose pointer) {
        if (pointer == null || s.inkDry) return;
        if (s.samples.isEmpty()) {
            s.samples.add(pointer.copy());
            return;
        }

        final var last = s.samples.getLast();
        if (!last.sameWorld(pointer)) return;

        final double spacing = s.width * SPACING_PER_WIDTH;
        if (last.position().distanceSquared(pointer.position()) >= spacing * spacing) {
            if (!charge(player, s, Math.sqrt(last.position().distanceSquared(pointer.position())))) {
                if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
                s.rubberband = null;
                return;
            }
            var previous = last;
            for (final var point : SurfacePath.between(raycaster, last, pointer, spacing)) {
                if (point == null) {
                    // The chord crossed a void: close this stroke and continue in a new one.
                    s = split(player, s);
                    previous = null;
                    continue;
                }
                if (previous != null) {
                    final var seg = renderer.spawn(previous, false, s.rgb, s.inkBright);
                    renderer.orient(seg, previous, point.position(), s.width);
                    s.preview.add(seg);
                }
                s.samples.add(point);
                previous = point;
            }
            if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
            s.rubberband = null;
            return;
        }

        if (s.rubberband == null || !s.rubberband.isValid()) {
            s.rubberband = renderer.spawn(last, false, s.rgb, s.inkBright);
        }
        renderer.orient(s.rubberband, last, pointer.position(), s.width);
    }

    public void handlePencil(@NotNull WPlayer player, boolean right) {
        final var pointer = raycaster.current(player);
        final var s = sessions.get(player.uuid());

        if (s == null) {
            if (!right || pointer == null) return;
            if (player.heldTool() == ToolType.PENCIL && !player.hasPermission(Permissions.TOOLS_USE)) {
                notifyNoPermission(player);
                return;
            }
            if (player.heldTool() == ToolType.QUILL && player.heldItemDurability() <= 1) {
                notifyDry(player);
                return;
            }
            if (player.sneaking()) startStraight(player, pointer);
            else startFreehand(player);
            return;
        }

        if (s.mode == DrawMode.STRAIGHT) {
            if (right) {
                if (pointer != null) commitStraight(player, s, pointer);
            } else {
                finishStraight(player, s);
            }
        } else {
            if (right) finishFreehand(player, s, pointer);
            else cancelSession(player, s);
        }
    }

    private void startFreehand(@NotNull WPlayer player) {
        final var s = new DrawSession(DrawMode.FREEHAND, colorOf(player), (float) radiusOf(player));
        applyInk(player, s);
        sessions.put(player.uuid(), s);
    }

    private void finishFreehand(@NotNull WPlayer player, @NotNull DrawSession s, @Nullable Pose pointer) {
        if (pointer != null && !s.inkDry && !s.samples.isEmpty()) {
            final var last = s.samples.getLast();
            if (last.sameWorld(pointer)
                    && last.position().distanceSquared(pointer.position()) > MIN_PARTIAL * MIN_PARTIAL) {
                final double length = Math.sqrt(last.position().distanceSquared(pointer.position()));
                if (!s.quillStroke) {
                    s.samples.add(pointer.copy());
                } else if (charge(player, s, length)) {
                    // Render the paid final partial so charged ink and drawn line match.
                    final var seg = renderer.spawn(last, false, s.rgb, s.inkBright);
                    renderer.orient(seg, last, pointer.position(), s.width);
                    s.preview.add(seg);
                    s.samples.add(pointer.copy());
                } else {
                    s.inkDry = true;
                }
            }
        }

        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.uuid());

        if (s.quillStroke && !s.preview.isEmpty()) {
            commitPreview(player, s);
            return;
        }

        discard(s);
        if (s.samples.size() < 2) return;

        final var points = SurfacePath.reglue(raycaster, Curve.chaikin(s.samples, config.smoothIterations()));
        final int count = renderStroke(player, s, points);
        if (count > 0) {
            final var first = points.getFirst();
            history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, count));
        }
    }

    private void startStraight(@NotNull WPlayer player, @NotNull Pose pointer) {
        final var s = new DrawSession(DrawMode.STRAIGHT, colorOf(player), (float) radiusOf(player));
        applyInk(player, s);
        s.anchor = pointer.copy();
        s.rubberband = renderer.spawn(s.anchor, false, s.rgb, s.inkBright);
        sessions.put(player.uuid(), s);
    }

    private void commitStraight(@NotNull WPlayer player, @NotNull DrawSession s, @NotNull Pose pointer) {
        if (!s.anchor.sameWorld(pointer)) return;
        if (!charge(player, s, Math.sqrt(s.anchor.position().distanceSquared(pointer.position())))) return;
        renderer.orient(s.rubberband, s.anchor, pointer.position(), s.width);
        s.rubberband.setPersistent(s.inkPersistent);
        s.rubberband.tag(s.strokeId, UUID.randomUUID(), s.rgb);
        s.committed.add(s.rubberband);
        s.anchor = pointer.copy();
        s.rubberband = renderer.spawn(s.anchor, false, s.rgb, s.inkBright);
    }

    private void finishStraight(@NotNull WPlayer player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.uuid());
        if (s.committed.isEmpty()) return;

        if (!s.inkPersistent) fade(s.committed);
        final var first = s.committed.getFirst();
        history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, s.committed.size()));
    }

    private void cancelSession(@NotNull WPlayer player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.uuid());
    }

    /** Finishes the stroke drawn so far and opens a fresh session carrying the ink state over. */
    private @NotNull DrawSession split(@NotNull WPlayer player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        s.rubberband = null;

        if (s.quillStroke && !s.preview.isEmpty()) {
            commitPreview(player, s);
        } else {
            discard(s);
            if (s.samples.size() >= 2) {
                final var points = SurfacePath.reglue(raycaster, Curve.chaikin(s.samples, config.smoothIterations()));
                final int count = renderStroke(player, s, points);
                if (count > 0) {
                    final var first = points.getFirst();
                    history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, count));
                }
            }
        }

        final var next = new DrawSession(DrawMode.FREEHAND, s.rgb, s.width);
        next.quillStroke = s.quillStroke;
        next.inkBright = s.inkBright;
        next.inkPersistent = s.inkPersistent;
        next.inkPending = s.inkPending;
        next.inkDry = s.inkDry;
        sessions.put(player.uuid(), next);
        return next;
    }

    private int renderStroke(@NotNull WPlayer player, @NotNull DrawSession s, @NotNull List<Pose> pts) {
        final var drawn = new ArrayList<SegmentHandle>(Math.max(0, pts.size() - 1));
        for (int i = 0; i < pts.size() - 1; i++) {
            final var from = pts.get(i);
            final var to = pts.get(i + 1).position();
            final double length = Math.sqrt(from.position().distanceSquared(to));
            if (s.quillStroke && !charge(player, s, length)) break;
            drawn.add(renderer.draw(from, to, s.rgb, s.strokeId,
                    UUID.randomUUID(), s.width, s.inkPersistent, s.inkBright));
        }
        if (!s.inkPersistent) fade(drawn);
        return drawn.size();
    }

    private void commitPreview(@NotNull WPlayer player, @NotNull DrawSession s) {
        WorldRef world = null;
        Vec3 position = null;
        int count = 0;
        for (final var seg : s.preview) {
            if (!seg.isValid()) continue;
            if (world == null) {
                world = seg.world();
                position = seg.position();
            }
            seg.setPersistent(s.inkPersistent);
            seg.tag(s.strokeId, UUID.randomUUID(), s.rgb);
            count++;
        }
        if (count == 0 || world == null || position == null) return;
        if (!s.inkPersistent) fade(s.preview);
        history.record(player, new Change.Draw(world, position, s.strokeId, count));
    }

    private void fade(@NotNull List<SegmentHandle> handles) {
        final long expireAt = System.currentTimeMillis() + config.markerTtlSeconds() * 1000L;
        for (final var handle : handles) {
            if (handle.isValid()) fading.add(new FadingSegment(handle, handle.snapshot().transform(), expireAt));
        }
    }

    private void applyInk(@NotNull WPlayer player, @NotNull DrawSession s) {
        s.quillStroke = player.heldTool() == ToolType.QUILL;
        if (!s.quillStroke) return;
        var ink = InkType.byId(player.heldToolInk());
        if (ink == null) ink = InkType.COMMON;
        s.inkBright = ink.bright();
        s.inkPersistent = ink.permanent();
    }

    private double minRadius(@NotNull WPlayer player) {
        if (player.heldTool() != ToolType.QUILL) return config.pencilMinRadius();
        return config.pencilMinRadius()
                * Quill.finenessMultiplier(player.heldToolTier(), player.heldToolQuality());
    }

    private double maxRadius(@NotNull WPlayer player) {
        return config.pencilMaxRadius() * player.heldToolTier();
    }

    /** Charges quill ink by the drawn length; the pencil draws for free. */
    private boolean charge(@NotNull WPlayer player, @NotNull DrawSession s, double length) {
        if (!s.quillStroke) return true;

        final double blocksPerDurability = config.markerBlocksPerDurability()
                * Quill.inkEfficiency(player.heldToolTier(), player.heldToolQuality());
        s.inkPending += length;
        while (s.inkPending >= blocksPerDurability) {
            if (player.heldItemDurability() <= 1) {
                s.inkDry = true;
                notifyDry(player);
                return false;
            }
            player.damageHeldItem(1);
            s.inkPending -= blocksPerDurability;
        }
        return true;
    }

    private void notifyDry(@NotNull WPlayer player) {
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.QUILL_DRY), NamedTextColor.RED));
    }

    private void notifyNoPermission(@NotNull WPlayer player) {
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.NO_PERMISSION), NamedTextColor.RED));
    }

    public void remove(@NotNull WPlayer player) {
        final var s = sessions.get(player.uuid());
        // Straight-mode segments are already committed to the world; finishing the
        // session keeps them undoable and lets non-permanent inks fade instead of
        // leaving them orphaned.
        if (s != null && s.mode == DrawMode.STRAIGHT) finishStraight(player, s);
        else discard(sessions.remove(player.uuid()));
        pencil.remove(player.uuid());
    }

    public void clearGlobals() {
        for (final var segment : fading) {
            if (segment.handle().isValid()) segment.handle().remove();
        }
        fading.clear();
    }

    private void discard(@Nullable DrawSession s) {
        if (s == null) return;
        for (final var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
