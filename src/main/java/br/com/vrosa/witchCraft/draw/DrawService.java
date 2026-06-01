package br.com.vrosa.witchCraft.draw;

import br.com.vrosa.witchCraft.raycast.RayHit;
import br.com.vrosa.witchCraft.raycast.Raycaster;
import br.com.vrosa.witchCraft.render.Curve;
import br.com.vrosa.witchCraft.render.SegmentRenderer;
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

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Raycaster raycaster;

    public DrawService(@NotNull Raycaster raycaster) {
        this.raycaster = raycaster;
    }

    public boolean isActive(@NotNull Player player) {
        return sessions.containsKey(player.getUniqueId());
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
            final var seg = SegmentRenderer.spawn(player.getWorld(), last.position(), false);
            SegmentRenderer.orient(seg, last, pointer.position());
            s.preview.add(seg);
            s.samples.add(pointer.copy());
            if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
            s.rubberband = null;
            return;
        }

        if (s.rubberband == null || !s.rubberband.isValid()) {
            s.rubberband = SegmentRenderer.spawn(player.getWorld(), last.position(), false);
        }
        SegmentRenderer.orient(s.rubberband, last, pointer.position());
    }

    void handleClick(@NotNull Player player, boolean right) {
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
        sessions.put(player.getUniqueId(), new DrawSession(DrawMode.FREEHAND));
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
        renderStroke(Curve.chaikin(s.samples, SMOOTH_ITERATIONS));
    }

    private void startStraight(@NotNull Player player, @NotNull RayHit pointer) {
        final var s = new DrawSession(DrawMode.STRAIGHT);
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(player.getWorld(), s.anchor.position(), false);
        sessions.put(player.getUniqueId(), s);
    }

    private void commitStraight(@NotNull DrawSession s, @NotNull RayHit pointer) {
        SegmentRenderer.orient(s.rubberband, s.anchor, pointer.position());
        s.rubberband.setPersistent(true);
        s.anchor = pointer.copy();
        s.rubberband = SegmentRenderer.spawn(s.anchor.position().getWorld(), s.anchor.position(), false);
    }

    private void finishStraight(@NotNull Player player, @NotNull DrawSession s) {
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
        sessions.remove(player.getUniqueId());
    }

    private void cancelSession(@NotNull Player player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.getUniqueId());
    }

    private void renderStroke(@NotNull List<RayHit> pts) {
        for (int i = 0; i < pts.size() - 1; i++) {
            SegmentRenderer.drawPermanent(pts.get(i), pts.get(i + 1).position());
        }
    }

    void remove(@NotNull Player player) {
        discard(sessions.remove(player.getUniqueId()));
        raycaster.clear(player);
    }

    private void discard(DrawSession s) {
        if (s == null) return;
        for (var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
