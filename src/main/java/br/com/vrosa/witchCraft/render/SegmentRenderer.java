package br.com.vrosa.witchCraft.render;

import br.com.vrosa.witchCraft.raycast.RayHit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SegmentRenderer {

    private static final float WIDTH = 0.05f;
    private static final float DEPTH = 0.03f;
    private static final float SURFACE_OFFSET = 0.005f;
    private static final float EPSILON = 1.0e-4f;

    private SegmentRenderer() {}

    public static @NotNull BlockDisplay spawn(@NotNull World world, @NotNull Location at, boolean persistent) {
        return world.spawn(at, BlockDisplay.class, d -> {
            d.setPersistent(persistent);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setBlock(Material.WHITE_CONCRETE.createBlockData());
            d.setTransformation(empty());
        });
    }

    public static void orient(@NotNull BlockDisplay display, @NotNull RayHit from, @NotNull Location to) {
        display.setTransformation(transform(from.position(), to, from.normal()));
    }

    public static void hide(@NotNull BlockDisplay display) {
        display.setTransformation(empty());
    }

    public static void drawPermanent(@NotNull RayHit from, @NotNull Location to) {
        final var transform = transform(from.position(), to, from.normal());
        from.position().getWorld().spawn(from.position(), BlockDisplay.class, d -> {
            d.setPersistent(true);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setBlock(Material.WHITE_CONCRETE.createBlockData());
            d.setTransformation(transform);
        });
    }

    private static @NotNull Transformation empty() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0f), new Quaternionf());
    }

    private static @NotNull Transformation transform(@NotNull Location from, @NotNull Location to, @NotNull Vector3f normal) {
        final var forward = new Vector3f(
                (float) (to.getX() - from.getX()),
                (float) (to.getY() - from.getY()),
                (float) (to.getZ() - from.getZ()));

        final float length = forward.length();
        if (length < EPSILON) return empty();
        forward.div(length);

        final var n = new Vector3f(normal);
        if (n.lengthSquared() < EPSILON) n.set(0f, 1f, 0f);
        n.normalize();

        final var right = new Vector3f(n).cross(forward);
        if (right.lengthSquared() < EPSILON) {
            final var rotation = new Quaternionf().rotationTo(new Vector3f(0f, 0f, 1f), forward);
            final var off = rotation.transform(new Vector3f(-WIDTH / 2f, -DEPTH / 2f, 0f), new Vector3f());
            return new Transformation(off, rotation, new Vector3f(WIDTH, DEPTH, length), new Quaternionf());
        }
        right.normalize();
        final var up = new Vector3f(forward).cross(right).normalize();

        final var rotation = new Quaternionf().setFromNormalized(new Matrix3f(
                right.x, right.y, right.z,
                up.x, up.y, up.z,
                forward.x, forward.y, forward.z));

        final var offset = rotation.transform(new Vector3f(-WIDTH / 2f, SURFACE_OFFSET, 0f), new Vector3f());
        return new Transformation(offset, rotation, new Vector3f(WIDTH, DEPTH, length), new Quaternionf());
    }
}
