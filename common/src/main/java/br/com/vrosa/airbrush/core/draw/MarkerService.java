package br.com.vrosa.airbrush.core.draw;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.glyph.GlyphStroke;
import br.com.vrosa.airbrush.core.glyph.GlyphStrokeStore;
import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.history.Change;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.render.Curve;
import br.com.vrosa.airbrush.core.render.SegmentRenderer;
import br.com.vrosa.airbrush.core.ui.ActionBars;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MarkerService {

    public static final int COLOR = 0xB784E0;

    private static final double SPACING_PER_WIDTH = 2.0;
    private static final double MIN_PARTIAL = 1.0e-3;
    private static final Key CLOSE_CHIME = Key.key("block.amethyst_block.chime");
    private static final float PULSE_SCALE = 1.3f;
    private static final long PULSE_MILLIS = 150;

    private record FadingSegment(@NotNull SegmentHandle handle, @NotNull Transform shape, long expireAt) {}

    private record Pulse(@NotNull SegmentHandle handle, @NotNull Transform shape, long until) {}

    private static final class Ink {
        double pending;
        int charged;
        boolean dry;
    }

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Map<UUID, Ink> ink = new HashMap<>();
    private final List<FadingSegment> fading = new ArrayList<>();
    private final List<Pulse> pulses = new ArrayList<>();

    private final Raycaster raycaster;
    private final History history;
    private final AirBrushConfig config;
    private final SegmentRenderer renderer;
    private final GlyphStrokeStore glyphStrokes;
    private final ActionBars bars;

    public MarkerService(@NotNull Platform platform, @NotNull Raycaster raycaster,
                         @NotNull History history, @NotNull AirBrushConfig config,
                         @NotNull GlyphStrokeStore glyphStrokes, @NotNull ActionBars bars) {
        this.raycaster = raycaster;
        this.history = history;
        this.config = config;
        this.renderer = new SegmentRenderer(platform, config);
        this.glyphStrokes = glyphStrokes;
        this.bars = bars;
    }

    public boolean isActive(@NotNull WPlayer player) {
        return sessions.containsKey(player.uuid());
    }

    public void handleMarker(@NotNull WPlayer player, boolean right) {
        final var pointer = raycaster.current(player);
        final var s = sessions.get(player.uuid());

        if (s == null) {
            if (!right || pointer == null) return;
            if (player.heldItemDurability() <= 1) {
                notifyDry(player);
                return;
            }
            sessions.put(player.uuid(), new DrawSession(DrawMode.FREEHAND, COLOR, config.markerWidth()));
            ink.put(player.uuid(), new Ink());
            return;
        }

        if (right) finish(player, s, pointer);
        else cancel(player, s);
    }

    public void confirmActive(@NotNull WPlayer player) {
        final var s = sessions.get(player.uuid());
        if (s == null) return;
        finish(player, s, null);
    }

    public void tick(@NotNull WPlayer player) {
        var s = sessions.get(player.uuid());
        if (s != null && player.heldTool() != ToolType.MARKER) {
            confirmActive(player);
            return;
        }
        if (s == null) return;
        if (player.heldTool() != ToolType.MARKER) return;

        final var state = ink.get(player.uuid());
        if (state == null || state.dry) return;

        final var pointer = raycaster.current(player);
        if (pointer == null) return;
        if (s.samples.isEmpty()) {
            s.samples.add(pointer.copy());
            return;
        }

        final var last = s.samples.getLast();
        if (!last.sameWorld(pointer)) return;

        final double spacing = s.width * SPACING_PER_WIDTH;
        final double distanceSquared = last.position().distanceSquared(pointer.position());
        if (distanceSquared >= spacing * spacing) {
            if (!charge(player, state, Math.sqrt(distanceSquared))) {
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
                    final var seg = renderer.spawn(previous, false, s.rgb, true);
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
            s.rubberband = renderer.spawn(last, false, s.rgb, true);
        }
        renderer.orient(s.rubberband, last, pointer.position(), s.width);
    }

    public void tickSegments() {
        final long now = System.currentTimeMillis();

        pulses.removeIf(pulse -> {
            if (!pulse.handle().isValid()) return true;
            if (now < pulse.until()) return false;
            pulse.handle().setTransform(pulse.shape());
            return true;
        });

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

        glyphStrokes.prune(now);
    }

    public void remove(@NotNull WPlayer player) {
        discard(sessions.remove(player.uuid()));
        refund(player, ink.remove(player.uuid()));
    }

    public void clearGlobals() {
        for (final var segment : fading) {
            if (segment.handle().isValid()) segment.handle().remove();
        }
        fading.clear();
        for (final var pulse : pulses) {
            if (pulse.handle().isValid()) pulse.handle().remove();
        }
        pulses.clear();
    }

    private void finish(@NotNull WPlayer player, @NotNull DrawSession s, @Nullable Pose pointer) {
        final var state = ink.remove(player.uuid());
        if (pointer != null && state != null && !state.dry && !s.samples.isEmpty()) {
            final var last = s.samples.getLast();
            if (last.sameWorld(pointer)
                    && last.position().distanceSquared(pointer.position()) > MIN_PARTIAL * MIN_PARTIAL) {
                final double length = Math.sqrt(last.position().distanceSquared(pointer.position()));
                if (!charge(player, state, length)) {
                    state.dry = true;
                } else {
                    // Render the paid final partial so charged ink and drawn line match.
                    final var seg = renderer.spawn(last, false, s.rgb, true);
                    renderer.orient(seg, last, pointer.position(), s.width);
                    s.preview.add(seg);
                    s.samples.add(pointer.copy());
                }
            }
        }

        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.uuid());

        if (!s.preview.isEmpty()) {
            commitPreview(player, s);
            return;
        }

        discard(s);
        if (s.samples.size() < 2) return;

        final var points = SurfacePath.reglue(raycaster, Curve.chaikin(s.samples, config.smoothIterations()));
        final long expireAt = System.currentTimeMillis() + config.markerTtlSeconds() * 1000L;
        final var drawn = new ArrayList<FadingSegment>(points.size() - 1);
        final var handles = new ArrayList<SegmentHandle>(points.size() - 1);
        for (int i = 0; i < points.size() - 1; i++) {
            if (state != null && state.dry) break;
            final var from = points.get(i);
            final var to = points.get(i + 1).position();
            final double length = Math.sqrt(from.position().distanceSquared(to));
            if (state != null && !charge(player, state, length)) break;

            final var handle = renderer.draw(from, to, s.rgb, s.strokeId, UUID.randomUUID(), s.width, false, true);
            final var segment = new FadingSegment(handle, handle.snapshot().transform(), expireAt);
            fading.add(segment);
            drawn.add(segment);
            handles.add(handle);
        }
        if (handles.isEmpty()) return;

        final var first = points.getFirst();
        glyphStrokes.add(new GlyphStroke(s.strokeId, player.uuid(), first.world(), s.samples, handles, expireAt));
        history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, handles.size()));

        if (GlyphAnalyzer.classifyStroke(s.samples, settings()).isPresent()) celebrate(player, drawn);
    }

    private void commitPreview(@NotNull WPlayer player, @NotNull DrawSession s) {
        final long expireAt = System.currentTimeMillis() + config.markerTtlSeconds() * 1000L;
        final var drawn = new ArrayList<FadingSegment>(s.preview.size());
        final var handles = new ArrayList<SegmentHandle>(s.preview.size());
        WorldRef world = null;
        Vec3 position = null;
        for (final var seg : s.preview) {
            if (!seg.isValid()) continue;
            if (world == null) {
                world = seg.world();
                position = seg.position();
            }
            seg.tag(s.strokeId, UUID.randomUUID(), s.rgb);
            final var fadingSeg = new FadingSegment(seg, seg.snapshot().transform(), expireAt);
            fading.add(fadingSeg);
            drawn.add(fadingSeg);
            handles.add(seg);
        }
        if (handles.isEmpty() || world == null || position == null) return;

        glyphStrokes.add(new GlyphStroke(s.strokeId, player.uuid(), world, s.samples, handles, expireAt));
        history.record(player, new Change.Draw(world, position, s.strokeId, handles.size()));
        if (GlyphAnalyzer.classifyStroke(s.samples, settings()).isPresent()) celebrate(player, drawn);
    }

    private void celebrate(@NotNull WPlayer player, @NotNull List<FadingSegment> drawn) {
        player.playSound(CLOSE_CHIME, 0.8f, 1.2f);
        final long until = System.currentTimeMillis() + PULSE_MILLIS;
        for (final var segment : drawn) {
            final var shape = segment.shape();
            final var scale = new Vector3f(shape.scale()).mul(PULSE_SCALE);
            segment.handle().setTransform(new Transform(shape.translation(), shape.leftRotation(),
                    scale, shape.rightRotation()));
            pulses.add(new Pulse(segment.handle(), shape, until));
        }
    }

    private @NotNull GlyphAnalyzer.Settings settings() {
        return new GlyphAnalyzer.Settings(config.glyphMinRadius(), config.glyphMaxRadius());
    }

    private void cancel(@NotNull WPlayer player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.uuid());
        refund(player, ink.remove(player.uuid()));
    }

    /** Finishes the stroke drawn so far and opens a fresh session; the ink state stays. */
    private @NotNull DrawSession split(@NotNull WPlayer player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        s.rubberband = null;
        if (!s.preview.isEmpty()) commitPreview(player, s);

        final var next = new DrawSession(DrawMode.FREEHAND, s.rgb, s.width);
        sessions.put(player.uuid(), next);
        return next;
    }

    private boolean charge(@NotNull WPlayer player, @NotNull Ink state, double length) {
        final double blocksPerDurability = config.markerBlocksPerDurability();
        state.pending += length;
        while (state.pending >= blocksPerDurability) {
            if (player.heldItemDurability() <= 1) {
                state.dry = true;
                notifyDry(player);
                return false;
            }
            player.damageHeldItem(1);
            state.charged++;
            state.pending -= blocksPerDurability;
        }
        return true;
    }

    private void refund(@NotNull WPlayer player, @Nullable Ink state) {
        if (state == null || state.charged <= 0) return;
        if (player.heldTool() != ToolType.MARKER) return;
        player.repairHeldItem(state.charged);
    }

    private void notifyDry(@NotNull WPlayer player) {
        bars.notice(player, Component.text(Messages.get(player.locale(), Messages.Key.MARKER_DRY), NamedTextColor.RED));
    }

    private void discard(@Nullable DrawSession s) {
        if (s == null) return;
        for (final var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
