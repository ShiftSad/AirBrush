package br.com.vrosa.airbrush.core.draw;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.history.Change;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.render.Curve;
import br.com.vrosa.airbrush.core.render.SegmentRenderer;
import br.com.vrosa.airbrush.platform.*;
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
    public static final float LINE_WIDTH = 0.02f;

    private static final long TTL_MILLIS = 60_000;
    private static final long SHRINK_MILLIS = 10_000;
    private static final double SPACING_PER_WIDTH = 2.0;
    private static final double MIN_PARTIAL = 1.0e-3;
    private static final double BLOCKS_PER_DURABILITY = 0.25;

    private record FadingSegment(@NotNull SegmentHandle handle, @NotNull Transform shape, long expireAt) {}

    private static final class Ink {
        double pending;
        int charged;
        boolean dry;
    }

    private final Map<UUID, DrawSession> sessions = new HashMap<>();
    private final Map<UUID, Ink> ink = new HashMap<>();
    private final List<FadingSegment> fading = new ArrayList<>();

    private final Raycaster raycaster;
    private final History history;
    private final AirBrushConfig config;
    private final SegmentRenderer renderer;

    public MarkerService(@NotNull Platform platform, @NotNull Raycaster raycaster,
                         @NotNull History history, @NotNull AirBrushConfig config) {
        this.raycaster = raycaster;
        this.history = history;
        this.config = config;
        this.renderer = new SegmentRenderer(platform, config);
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
            sessions.put(player.uuid(), new DrawSession(DrawMode.FREEHAND, COLOR, LINE_WIDTH));
            ink.put(player.uuid(), new Ink());
            return;
        }

        if (right) finish(player, s, pointer);
        else cancel(player, s);
    }

    public void tick(@NotNull WPlayer player) {
        final var s = sessions.get(player.uuid());
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
            final var seg = renderer.spawn(last.world(), last.position(), false, s.rgb, true);
            renderer.orient(seg, last, pointer.position(), s.width);
            s.preview.add(seg);
            s.samples.add(pointer.copy());
            if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
            s.rubberband = null;
            return;
        }

        if (s.rubberband == null || !s.rubberband.isValid()) {
            s.rubberband = renderer.spawn(last.world(), last.position(), false, s.rgb, true);
        }
        renderer.orient(s.rubberband, last, pointer.position(), s.width);
    }

    public void tickSegments() {
        if (fading.isEmpty()) return;

        final long now = System.currentTimeMillis();
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
            if (remaining >= SHRINK_MILLIS) continue;

            final float factor = (float) remaining / SHRINK_MILLIS;
            final var shape = segment.shape();
            final var scale = new Vector3f(shape.scale());
            scale.x *= factor;
            scale.y *= factor;
            segment.handle().setTransform(new Transform(shape.translation(), shape.leftRotation(),
                    scale, shape.rightRotation()));
        }
    }

    public void remove(@NotNull WPlayer player) {
        discard(sessions.remove(player.uuid()));
        refund(player, ink.remove(player.uuid()));
    }

    private void finish(@NotNull WPlayer player, @NotNull DrawSession s, @Nullable Pose pointer) {
        final var state = ink.remove(player.uuid());
        if (pointer != null && (state == null || !state.dry) && !s.samples.isEmpty()) {
            final var last = s.samples.getLast();
            if (last.sameWorld(pointer)
                    && last.position().distanceSquared(pointer.position()) > MIN_PARTIAL * MIN_PARTIAL) {
                s.samples.add(pointer.copy());
            }
        }
        discard(s);
        sessions.remove(player.uuid());
        if (s.samples.size() < 2) return;

        final var points = Curve.chaikin(s.samples, config.smoothIterations());
        final long expireAt = System.currentTimeMillis() + TTL_MILLIS;
        for (int i = 0; i < points.size() - 1; i++) {
            final var handle = renderer.draw(points.get(i), points.get(i + 1).position(),
                    s.rgb, s.strokeId, UUID.randomUUID(), s.width, false, true);
            fading.add(new FadingSegment(handle, handle.snapshot().transform(), expireAt));
        }

        final var first = points.getFirst();
        history.record(player, new Change.Draw(first.world(), first.position(), s.strokeId, points.size() - 1));
    }

    private void cancel(@NotNull WPlayer player, @NotNull DrawSession s) {
        discard(s);
        sessions.remove(player.uuid());
        refund(player, ink.remove(player.uuid()));
    }

    private boolean charge(@NotNull WPlayer player, @NotNull Ink state, double length) {
        state.pending += length;
        while (state.pending >= BLOCKS_PER_DURABILITY) {
            if (player.heldItemDurability() <= 1) {
                state.dry = true;
                notifyDry(player);
                return false;
            }
            player.damageHeldItem(1);
            state.charged++;
            state.pending -= BLOCKS_PER_DURABILITY;
        }
        return true;
    }

    private void refund(@NotNull WPlayer player, @Nullable Ink state) {
        if (state == null || state.charged <= 0) return;
        if (player.heldTool() != ToolType.MARKER) return;
        player.repairHeldItem(state.charged);
    }

    private void notifyDry(@NotNull WPlayer player) {
        player.actionBar(Component.text(Messages.get(player.locale(), Messages.Key.MARKER_DRY), NamedTextColor.RED));
    }

    private void discard(@Nullable DrawSession s) {
        if (s == null) return;
        for (final var seg : s.preview) if (seg.isValid()) seg.remove();
        if (s.rubberband != null && s.rubberband.isValid()) s.rubberband.remove();
    }
}
