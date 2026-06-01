package br.com.vrosa.witchcraft.draw;

import br.com.vrosa.witchcraft.history.Change;
import br.com.vrosa.witchcraft.history.History;
import br.com.vrosa.witchcraft.raycast.RayHit;
import br.com.vrosa.witchcraft.raycast.Raycaster;
import br.com.vrosa.witchcraft.render.Curve;
import br.com.vrosa.witchcraft.render.SegmentRenderer;
import br.com.vrosa.witchcraft.render.Segments;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DrawService {

    private static final double MIN_SPACING = 0.2;
    private static final double MIN_PARTIAL = 1.0e-3;
    private static final int SMOOTH_ITERATIONS = 2;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Map<UUID, Integer> pencil = new HashMap<>();
    private final Raycaster raycaster;
    private final History history;

    public DrawService(@NotNull Raycaster raycaster, @NotNull History history) {
        this.raycaster = raycaster;
        this.history = history;
    }

    public boolean isActive(@NotNull Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void setColor(@NotNull Player player, int rgb) {
        pencil.put(player.getUniqueId(), rgb & 0xFFFFFF);
    }

    public int colorOf(@NotNull Player player) {
        return pencil.getOrDefault(player.getUniqueId(), DEFAULT_COLOR);
    }

    public void tick(@NotNull Player player) {
        final var session = sessions.get(player.getUniqueId());
        if (session == null) return;

        final var pointer = raycaster.current(player);
        if (session.mode == DrawMode.STRAIGHT) tickStraight(session, pointer);
        else tickFreehand(player, session, pointer);
    }

    private void tickStraight(@NotNull DrawSession s, RayHit pointer) {
        if (s.rubberband == null || !s.rubberband.isValid()) return;
        if (pointer == null) {
            SegmentRenderer.hide(s.rubberband);
            return;
        }
        if (!s.anchor.position().getWorld().equals(pointer.position().getWorld())) return;
        SegmentRenderer.orient(s.rubberband, s.anchor, pointer.position());
    }

    private void tickFreehand(@NotNull Player player, @NotNull DrawSession s, RayHit pointer) {
        if (pointer == null) return;
        if (s.samples.isEmpty()) {
            s.samples.add(pointer.copy());
            return;
        }

        final var last = s.samples.getLast();
        if (!last.position().getWorld().equals(pointer.position().getWorld())) return;

        if (last.position().distanceSquared(pointer.position()) >= MIN_SPACING * MIN_SPACING) {
            final var seg = SegmentRenderer.spawn(player.getWorld(), last.position(), false, s.rgb);
            SegmentRenderer.orient(seg, last, pointer.position());
            s.preview.add(seg);
            s.samples.add(pointer.copy());
            if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
            s.rubberband = null;
            return;
        }

        if (s.rubberband == null || !s.rubberband.isValid()) {
            s.rubberband = SegmentRenderer.spawn(player.getWorld(), last.position(), false, s.rgb);
        }
        SegmentRenderer.orient(s.rubberband, last, pointer.position());
    }

    void handlePencil(@NotNull Player player, boolean right) {
        final var pointer = raycaster.current(player);
        final var s = sessions.get(player.getUniqueId());

        if (s == null) {
            if (!right || pointer == null) return;
            if (player.isSneaking()) startStraight(player, pointer);
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

    private void startFreehand(@NotNull Player player) {
        sessions.put(player.getUniqueId(), new DrawSession(DrawMode.FREEHAND, colorOf(player)));
    }

    private void finishFreehand(@NotNull Player player, @NotNull DrawSession s, RayHit pointer) {
        if (pointer != null && !s.samples.isEmpty()) {
            final var last = s.samples.getLast();
            if (last.position().getWorld().equals(pointer.position().getWorld())
                    && last.position().distanceSquared(pointer.position()) > MIN_PARTIAL * MIN_PARTIAL) {
                s.samples.add(pointer.copy());
            }
        }
        discard(s);
        sessions.remove(player.getUniqueId());
        if (s.samples.size() < 2) return;

        final var points = Curve.chaikin(s.samples, SMOOTH_ITERATIONS);
        final int count = renderStroke(points, s.rgb, s.strokeId);
        if (count > 0) {
            history.record(player, new Change.Draw(points.getFirst().position(), s.strokeId, count));
        }
    }

    private void startStraight(@NotNull Player player, @NotNull RayHit pointer) {
        final var s = new DrawSession(DrawMode.STRAIGHT, colorOf(player));
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(player.getWorld(), s.anchor.position(), false, s.rgb);
        sessions.put(player.getUniqueId(), s);
    }

    private void commitStraight(@NotNull DrawSession s, @NotNull RayHit pointer) {
        SegmentRenderer.orient(s.rubberband, s.anchor, pointer.position());
        s.rubberband.setPersistent(true);
        Segments.tag(s.rubberband, s.strokeId, UUID.randomUUID(), s.rgb);
        s.committed.add(s.rubberband);
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(s.anchor.position().getWorld(), s.anchor.position(), false, s.rgb);
    }

    private void finishStraight(@NotNull Player player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.getUniqueId());
        if (!s.committed.isEmpty()) {
            history.record(player, new Change.Draw(
                    s.committed.getFirst().getLocation(), s.strokeId, s.committed.size()));
        }
    }

    private void cancelSession(@NotNull Player player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.getUniqueId());
    }

    private int renderStroke(@NotNull List<RayHit> pts, int rgb, @NotNull UUID strokeId) {
        for (int i = 0; i < pts.size() - 1; i++) {
            SegmentRenderer.drawPermanent(pts.get(i), pts.get(i + 1).position(), rgb, strokeId, strokeId);
        }
        return Math.max(0, pts.size() - 1);
    }

    void remove(@NotNull Player player) {
        discard(sessions.remove(player.getUniqueId()));
        pencil.remove(player.getUniqueId());
        raycaster.clear(player);
    }

    private void discard(DrawSession s) {
        if (s == null) return;
        for (var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
