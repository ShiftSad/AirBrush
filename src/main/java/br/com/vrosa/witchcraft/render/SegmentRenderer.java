package br.com.vrosa.witchcraft.render;

import br.com.vrosa.witchcraft.raycast.RayHit;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public final class SegmentRenderer {

    private static final float DEPTH = 0.03f;
    private static final float SURFACE_OFFSET = 0.005f;
    private static final float EPSILON = 1.0e-4f;

    private static final Key MODEL = Key.key("witchcraft", "drawing_segment");

    private SegmentRenderer() {}

    private static @NotNull ItemStack item(int rgb) {
        final var item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, MODEL);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addColor(Color.fromRGB(rgb & 0xFFFFFF))
                .build());
        return item;
    }

    public static @NotNull ItemDisplay spawn(@NotNull World world, @NotNull Location at, boolean persistent, int rgb) {
        return world.spawn(at, ItemDisplay.class, d -> {
            d.setPersistent(persistent);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setItemStack(item(rgb));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(empty());
        });
    }

    public static void orient(@NotNull ItemDisplay display, @NotNull RayHit from, @NotNull Location to, float width) {
        display.setTransformation(transform(from.position(), to, from.normal(), width));
    }

    public static void hide(@NotNull ItemDisplay display) {
        display.setTransformation(empty());
    }

    public static void drawPermanent(@NotNull RayHit from, @NotNull Location to, int rgb,
                                     @NotNull UUID strokeId, @NotNull UUID segmentId, float width) {
        final var display = spawn(from.position().getWorld(), from.position(), true, rgb);
        display.setTransformation(transform(from.position(), to, from.normal(), width));
        Segments.tag(display, strokeId, segmentId, rgb);
    }

    private static @NotNull Transformation empty() {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0f), new Quaternionf());
    }

    private static @NotNull Transformation transform(@NotNull Location from, @NotNull Location to, @NotNull Vector3f normal, float width) {
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
            final var off = rotation.transform(new Vector3f(0f, 0f, length / 2f), new Vector3f());
            return new Transformation(off, rotation, new Vector3f(width, DEPTH, length), new Quaternionf());
        }
        right.normalize();
        final var up = new Vector3f(forward).cross(right).normalize();

        final var rotation = new Quaternionf().setFromNormalized(new Matrix3f(
                right.x, right.y, right.z,
                up.x, up.y, up.z,
                forward.x, forward.y, forward.z));

        final var offset = rotation.transform(
                new Vector3f(0f, SURFACE_OFFSET + DEPTH / 2f, length / 2f), new Vector3f());
        return new Transformation(offset, rotation, new Vector3f(width, DEPTH, length), new Quaternionf());
    }
}
