package br.com.vrosa.witchcraft.render;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static br.com.vrosa.witchcraft.keys.WitchCraftKey.*;

public final class Segments {

    private static final NamespacedKey STROKE = key(STROKE_ID.getKey());
    private static final NamespacedKey SEGMENT = key(SEGMENT_ID.getKey());
    private static final NamespacedKey COLOR = key(SEGMENT_COLOR.getKey());

    private Segments() {}

    private static @NotNull NamespacedKey key(@NotNull String value) {
        return new NamespacedKey(WITCHCRAFT_KEY.getKey(), value);
    }

    public static void tag(@NotNull ItemDisplay display, @NotNull UUID strokeId, @NotNull UUID segmentId, int rgb) {
        final var pdc = display.getPersistentDataContainer();
        pdc.set(STROKE, PersistentDataType.STRING, strokeId.toString());
        pdc.set(SEGMENT, PersistentDataType.STRING, segmentId.toString());
        pdc.set(COLOR, PersistentDataType.INTEGER, rgb & 0xFFFFFF);
    }

    public static boolean isSegment(@NotNull ItemDisplay display) {
        return display.getPersistentDataContainer().has(STROKE, PersistentDataType.STRING);
    }

    public static @Nullable UUID strokeId(@NotNull ItemDisplay display) {
        final var raw = display.getPersistentDataContainer().get(STROKE, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    public static @Nullable UUID segmentId(@NotNull ItemDisplay display) {
        final var raw = display.getPersistentDataContainer().get(SEGMENT, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    public static int colorOf(@NotNull ItemDisplay display) {
        final var raw = display.getPersistentDataContainer().get(COLOR, PersistentDataType.INTEGER);
        return raw == null ? 0xFFFFFF : raw;
    }

    public static @NotNull List<ItemDisplay> within(@NotNull Location center, double radius) {
        final var world = center.getWorld();
        if (world == null) return List.of();

        final double r2 = radius * radius;
        final var result = new ArrayList<ItemDisplay>();
        for (final var entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof ItemDisplay display
                    && isSegment(display)
                    && display.getLocation().distanceSquared(center) <= r2) {
                result.add(display);
            }
        }
        return result;
    }

    public static @NotNull List<ItemDisplay> byStrokes(@NotNull Location near, @NotNull Set<UUID> strokeIds) {
        final var world = near.getWorld();
        if (world == null || strokeIds.isEmpty()) return List.of();

        final var result = new ArrayList<ItemDisplay>();
        for (final var display : world.getEntitiesByClass(ItemDisplay.class)) {
            if (!isSegment(display)) continue;
            final var raw = display.getPersistentDataContainer().get(STROKE, PersistentDataType.STRING);
            if (raw != null && strokeIds.contains(UUID.fromString(raw))) result.add(display);
        }
        return result;
    }

    public static @NotNull Snapshot snapshot(@NotNull ItemDisplay display) {
        return new Snapshot(
                display.getLocation().clone(),
                display.getTransformation(),
                display.getItemStack(),
                display.getBrightness(),
                strokeIdOrRandom(display),
                segmentIdOrRandom(display),
                colorOf(display));
    }

    public static void restore(@NotNull Snapshot snapshot) {
        final var world = snapshot.location().getWorld();
        world.spawn(snapshot.location(), ItemDisplay.class, d -> {
            d.setPersistent(true);
            if (snapshot.brightness() != null) d.setBrightness(snapshot.brightness());
            if (snapshot.itemStack() != null) d.setItemStack(snapshot.itemStack());
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(snapshot.transformation());
            tag(d, snapshot.strokeId(), snapshot.segmentId(), snapshot.rgb());
        });
    }

    private static @NotNull UUID strokeIdOrRandom(@NotNull ItemDisplay display) {
        final var id = strokeId(display);
        return id == null ? UUID.randomUUID() : id;
    }

    private static @NotNull UUID segmentIdOrRandom(@NotNull ItemDisplay display) {
        final var id = segmentId(display);
        return id == null ? UUID.randomUUID() : id;
    }

    public record Snapshot(
            @NotNull Location location,
            @NotNull Transformation transformation,
            @Nullable ItemStack itemStack,
            @Nullable Display.Brightness brightness,
            @NotNull UUID strokeId,
            @NotNull UUID segmentId,
            int rgb) {}
}
