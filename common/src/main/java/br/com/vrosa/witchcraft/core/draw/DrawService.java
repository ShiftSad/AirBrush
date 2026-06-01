package br.com.vrosa.witchcraft.core.draw;

import br.com.vrosa.witchcraft.core.history.Change;
import br.com.vrosa.witchcraft.core.history.History;
import br.com.vrosa.witchcraft.core.render.Curve;
import br.com.vrosa.witchcraft.core.render.SegmentRenderer;
import br.com.vrosa.witchcraft.platform.Platform;
import br.com.vrosa.witchcraft.platform.Pose;
import br.com.vrosa.witchcraft.platform.Raycaster;
import br.com.vrosa.witchcraft.platform.Sounds;
import br.com.vrosa.witchcraft.platform.ToolType;
import br.com.vrosa.witchcraft.platform.WPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DrawService {

    private static final double SPACING_PER_WIDTH = 2.0;
    private static final double MIN_PARTIAL = 1.0e-3;
    private static final int SMOOTH_ITERATIONS = 2;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private static final double DEFAULT_RADIUS = 0.05;
    private static final double MIN_RADIUS = 0.01;
    private static final double MAX_RADIUS = 0.1;
    private static final double RADIUS_STEP = 0.01;

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Map<UUID, Integer> pencil = new HashMap<>();
    private final Map<UUID, Double> radii = new HashMap<>();

    private final Platform platform;
    private final Raycaster raycaster;
    private final History history;

    public DrawService(@NotNull Platform platform, @NotNull Raycaster raycaster, @NotNull History history) {
        this.platform = platform;
        this.raycaster = raycaster;
        this.history = history;
    }

    public boolean isActive(@NotNull WPlayer player) {
        return sessions.containsKey(player.uuid());
    }

    public boolean isHolding(@NotNull WPlayer player) {
        return player.heldTool() == ToolType.PENCIL;
    }

    public void setColor(@NotNull WPlayer player, int rgb) {
        pencil.put(player.uuid(), rgb & 0xFFFFFF);
    }

    public int colorOf(@NotNull WPlayer player) {
        return pencil.getOrDefault(player.uuid(), DEFAULT_COLOR);
    }

    public double radiusOf(@NotNull WPlayer player) {
        return radii.getOrDefault(player.uuid(), DEFAULT_RADIUS);
    }

    public void changeRadius(@NotNull WPlayer player, int direction) {
        final double current = radiusOf(player);
        final double next = Math.clamp(current + direction * RADIUS_STEP, MIN_RADIUS, MAX_RADIUS);
        radii.put(player.uuid(), next);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.5f, next > current ? 1.8f : 1.0f);
        player.actionBar(actionBar(player, next));
    }

    public void tick(@NotNull WPlayer player) {
        if (isHolding(player)) player.actionBar(actionBar(player, radiusOf(player)));

        final var session = sessions.get(player.uuid());
        if (session == null) return;

        final var pointer = raycaster.current(player);
        if (session.mode == DrawMode.STRAIGHT) tickStraight(session, pointer);
        else tickFreehand(session, pointer);
    }

    private @NotNull Component actionBar(@NotNull WPlayer player, double radius) {
        return Component.text("Lápis", NamedTextColor.GRAY)
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Cor", TextColor.color(colorOf(player))))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Espessura: ", NamedTextColor.GRAY))
                .append(Component.text(String.format(Locale.ROOT, "%.2f", radius), NamedTextColor.WHITE));
    }

    private void tickStraight(@NotNull DrawSession s, @Nullable Pose pointer) {
        if (s.rubberband == null || !s.rubberband.isValid()) return;
        if (pointer == null) {
            SegmentRenderer.hide(s.rubberband);
            return;
        }
        if (!s.anchor.sameWorld(pointer)) return;
        SegmentRenderer.orient(s.rubberband, s.anchor, pointer.position(), s.width);
    }

    private void tickFreehand(@NotNull DrawSession s, @Nullable Pose pointer) {
        if (pointer == null) return;
        if (s.samples.isEmpty()) {
            s.samples.add(pointer.copy());
            return;
        }

        final var last = s.samples.getLast();
        if (!last.sameWorld(pointer)) return;

        final double spacing = s.width * SPACING_PER_WIDTH;
        if (last.position().distanceSquared(pointer.position()) >= spacing * spacing) {
            final var seg = SegmentRenderer.spawn(platform, last.world(), last.position(), false, s.rgb);
            SegmentRenderer.orient(seg, last, pointer.position(), s.width);
            s.preview.add(seg);
            s.samples.add(pointer.copy());
            if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
            s.rubberband = null;
            return;
        }

        if (s.rubberband == null || !s.rubberband.isValid()) {
            s.rubberband = SegmentRenderer.spawn(platform, last.world(), last.position(), false, s.rgb);
        }
        SegmentRenderer.orient(s.rubberband, last, pointer.position(), s.width);
    }

    public void handlePencil(@NotNull WPlayer player, boolean right) {
        final var pointer = raycaster.current(player);
        final var s = sessions.get(player.uuid());

        if (s == null) {
            if (!right || pointer == null) return;
            if (player.sneaking()) startStraight(player, pointer);
            else startFreehand(player);
            return;
        }

        if (s.mode == DrawMode.STRAIGHT) {
            if (right) {
                if (pointer != null) commitStraight(s, pointer);
            } else {
                finishStraight(player, s);
            }
        } else {
            if (right) finishFreehand(player, s, pointer);
            else cancelSession(player, s);
        }
    }

    private void startFreehand(@NotNull WPlayer player) {
        sessions.put(player.uuid(), new DrawSession(DrawMode.FREEHAND, colorOf(player), (float) radiusOf(player)));
    }

    private void finishFreehand(@NotNull WPlayer player, @NotNull DrawSession s, @Nullable Pose pointer) {
        if (pointer != null && !s.samples.isEmpty()) {
            final var last = s.samples.getLast();
            if (last.sameWorld(pointer)
                    && last.position().distanceSquared(pointer.position()) > MIN_PARTIAL * MIN_PARTIAL) {
                s.samples.add(pointer.copy());
            }
        }
        discard(s);
        sessions.remove(player.uuid());
        if (s.samples.size() < 2) return;

        final var points = Curve.chaikin(s.samples, SMOOTH_ITERATIONS);
        final int count = renderStroke(points, s.rgb, s.strokeId, s.width);
        if (count > 0) {
            final var first = points.getFirst();
            history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, count));
        }
    }

    private void startStraight(@NotNull WPlayer player, @NotNull Pose pointer) {
        final var s = new DrawSession(DrawMode.STRAIGHT, colorOf(player), (float) radiusOf(player));
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(platform, pointer.world(), s.anchor.position(), false, s.rgb);
        sessions.put(player.uuid(), s);
    }

    private void commitStraight(@NotNull DrawSession s, @NotNull Pose pointer) {
        SegmentRenderer.orient(s.rubberband, s.anchor, pointer.position(), s.width);
        s.rubberband.setPersistent(true);
        s.rubberband.tag(s.strokeId, UUID.randomUUID(), s.rgb);
        s.committed.add(s.rubberband);
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(platform, s.anchor.world(), s.anchor.position(), false, s.rgb);
    }

    private void finishStraight(@NotNull WPlayer player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.uuid());
        if (!s.committed.isEmpty()) {
            final var first = s.committed.getFirst();
            history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, s.committed.size()));
        }
    }

    private void cancelSession(@NotNull WPlayer player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.uuid());
    }

    private int renderStroke(@NotNull List<Pose> pts, int rgb, @NotNull UUID strokeId, float width) {
        for (int i = 0; i < pts.size() - 1; i++) {
            SegmentRenderer.drawPermanent(platform, pts.get(i), pts.get(i + 1).position(), rgb, strokeId, strokeId, width);
        }
        return Math.max(0, pts.size() - 1);
    }

    public void remove(@NotNull WPlayer player) {
        discard(sessions.remove(player.uuid()));
        pencil.remove(player.uuid());
        radii.remove(player.uuid());
        raycaster.clear(player);
    }

    private void discard(@Nullable DrawSession s) {
        if (s == null) return;
        for (final var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
