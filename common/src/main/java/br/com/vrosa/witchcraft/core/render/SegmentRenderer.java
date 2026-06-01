package br.com.vrosa.witchcraft.core.render;

import br.com.vrosa.witchcraft.platform.Platform;
import br.com.vrosa.witchcraft.platform.Pose;
import br.com.vrosa.witchcraft.platform.SegmentHandle;
import br.com.vrosa.witchcraft.platform.Transform;
import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WorldRef;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public final class SegmentRenderer {

    private static final float DEPTH = 0.03f;
    private static final float SURFACE_OFFSET = 0.005f;
    private static final float EPSILON = 1.0e-4f;

    private SegmentRenderer() {}

    public static @NotNull SegmentHandle spawn(@NotNull Platform platform, @NotNull WorldRef world,
                                               @NotNull Vec3 at, boolean persistent, int rgb) {
        return platform.spawnSegment(world, at, rgb, persistent);
    }

    public static void orient(@NotNull SegmentHandle display, @NotNull Pose from, @NotNull Vec3 to, float width) {
        display.setTransform(transform(from.position(), to, from.normal(), width));
    }

    public static void hide(@NotNull SegmentHandle display) {
        display.setTransform(Transform.empty());
    }

    public static void drawPermanent(@NotNull Platform platform, @NotNull Pose from, @NotNull Vec3 to,
                                     int rgb, @NotNull UUID strokeId, @NotNull UUID segmentId, float width) {
        final var display = platform.spawnSegment(from.world(), from.position(), rgb, true);
        display.setTransform(transform(from.position(), to, from.normal(), width));
        display.tag(strokeId, segmentId, rgb);
    }

    private static @NotNull Transform transform(@NotNull Vec3 from, @NotNull Vec3 to, @NotNull Vector3f normal, float width) {
        final var forward = new Vector3f(
                (float) (to.x() - from.x()),
                (float) (to.y() - from.y()),
                (float) (to.z() - from.z()));

        final float length = forward.length();
        if (length < EPSILON) return Transform.empty();
        forward.div(length);

        final var n = new Vector3f(normal);
        if (n.lengthSquared() < EPSILON) n.set(0f, 1f, 0f);
        n.normalize();

        final var right = new Vector3f(n).cross(forward);
        if (right.lengthSquared() < EPSILON) {
            final var rotation = new Quaternionf().rotationTo(new Vector3f(0f, 0f, 1f), forward);
            final var off = rotation.transform(new Vector3f(0f, 0f, length / 2f), new Vector3f());
            return new Transform(off, rotation, new Vector3f(width, DEPTH, length), new Quaternionf());
        }
        right.normalize();
        final var up = new Vector3f(forward).cross(right).normalize();

        final var rotation = new Quaternionf().setFromNormalized(new Matrix3f(
                right.x, right.y, right.z,
                up.x, up.y, up.z,
                forward.x, forward.y, forward.z));

        final var offset = rotation.transform(
                new Vector3f(0f, SURFACE_OFFSET + DEPTH / 2f, length / 2f), new Vector3f());
        return new Transform(offset, rotation, new Vector3f(width, DEPTH, length), new Quaternionf());
    }
}
