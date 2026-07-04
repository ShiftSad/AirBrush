package br.com.vrosa.airbrush.core.draw;

import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Raycaster;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps fast strokes glued to the terrain. When two consecutive samples are far
 * apart (quick swipe, or the surface stepped up/down), the straight chord between
 * them is subdivided and every intermediate point is dropped back onto the surface —
 * first along the blended normal, then straight down (the line "falls"). Where no
 * surface exists at all, a {@code null} gap marker is emitted instead of a floating
 * point: the stroke must be split there, never drawn across the void.
 */
final class SurfacePath {

    /** How far above the surface the projection ray starts, along the normal. */
    private static final double LIFT = 0.75;
    /** How deep the projection ray searches for a surface to land on. */
    private static final double DROP = 3.0;
    /** Max horizontal distance between consecutive points; farther apart means a gap. */
    private static final double MAX_HORIZONTAL_LINK = 1.5;
    /** Search depth when re-gluing smoothed points — they only drift a little off the surface. */
    private static final double RESNAP_DROP = 1.5;
    /** Raycast budget per pointer update. */
    private static final int MAX_STEPS = 48;

    private static final Vector3f DOWN = new Vector3f(0f, -1f, 0f);

    private SurfacePath() {}

    /**
     * Points from {@code from} (exclusive) to {@code to} (inclusive), spaced at most
     * {@code spacing} apart along the chord, each glued to the nearest surface.
     * A {@code null} entry marks a gap the stroke must not cross.
     */
    static @NotNull List<@Nullable Pose> between(@NotNull Raycaster raycaster, @NotNull Pose from, @NotNull Pose to,
                                                 double spacing) {
        final double distance = Math.sqrt(from.position().distanceSquared(to.position()));
        final int steps = Math.clamp((int) Math.ceil(distance / spacing), 1, MAX_STEPS);

        final var result = new ArrayList<Pose>(steps);
        var previous = from;
        for (int i = 1; i < steps; i++) {
            final var hit = glue(raycaster, from, to, (double) i / steps);
            if (hit == null || !linked(previous, hit)) {
                if (previous != null) result.add(null);
                previous = null;
                continue;
            }
            // Collapse points piling up on the same spot (e.g. at the base of a wall).
            if (previous != null && hit.position().distanceSquared(previous.position()) < spacing * spacing * 0.25) {
                continue;
            }
            result.add(hit);
            previous = hit;
        }

        if (!linked(previous, to)) result.add(null);
        result.add(to.copy());
        return result;
    }

    /**
     * Re-glues an already sampled polyline after smoothing: Chaikin cuts corners,
     * which pulls points at concave edges into the air; dropping each one back onto
     * the surface restores the hug. Points with no surface nearby are kept as-is.
     */
    static @NotNull List<Pose> reglue(@NotNull Raycaster raycaster, @NotNull List<Pose> points) {
        final var result = new ArrayList<Pose>(points.size());
        for (final var point : points) {
            final var glued = glue(raycaster, point.world(), point.position(), point.normal(), RESNAP_DROP);
            result.add(glued != null ? glued : point);
        }
        return result;
    }

    private static @Nullable Pose glue(@NotNull Raycaster raycaster, @NotNull Pose from, @NotNull Pose to, double t) {
        final var chord = from.position().lerp(to.position(), t);
        final var normal = new Vector3f(from.normal()).lerp(to.normal(), (float) t);
        return glue(raycaster, from.world(), chord, normal, DROP);
    }

    private static @Nullable Pose glue(@NotNull Raycaster raycaster, @NotNull WorldRef world,
                                       @NotNull Vec3 position, @NotNull Vector3f normal, double drop) {
        final var direction = new Vector3f(normal);
        if (direction.lengthSquared() > 1.0e-6f) {
            direction.normalize();
            final var lifted = new Vec3(
                    position.x() + direction.x * LIFT,
                    position.y() + direction.y * LIFT,
                    position.z() + direction.z * LIFT);
            final var hit = raycaster.project(world, lifted, new Vector3f(direction).negate(), LIFT + drop);
            if (usable(hit, lifted)) return hit;
        }

        // Gravity fallback: let the point fall onto whatever ground lies below.
        final var above = new Vec3(position.x(), position.y() + LIFT, position.z());
        final var fall = raycaster.project(world, above, DOWN, LIFT + drop);
        return usable(fall, above) ? fall : null;
    }

    /** A hit at the ray origin means it started inside a block (concave corner) — not a surface. */
    private static boolean usable(@Nullable Pose hit, @NotNull Vec3 origin) {
        return hit != null && hit.position().distanceSquared(origin) > 1.0e-4;
    }

    /** Vertical hops are fine (the segment hugs the wall face); horizontal leaps are gaps. */
    private static boolean linked(@Nullable Pose previous, @NotNull Pose next) {
        if (previous == null) return true;
        final double dx = next.position().x() - previous.position().x();
        final double dz = next.position().z() - previous.position().z();
        return dx * dx + dz * dz <= MAX_HORIZONTAL_LINK * MAX_HORIZONTAL_LINK;
    }
}
