package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.paper.item.Items;
import br.com.vrosa.airbrush.paper.item.Keys;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BukkitPlatform implements Platform {

    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

    @Override
    public @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb, boolean persistent) {
        return new BukkitSegment(itemDisplay((BukkitWorld) world, at, Items.segment(rgb), persistent, Transform.empty()));
    }

    @Override
    public @NotNull CursorHandle spawnCursor(@NotNull WorldRef world, @NotNull Vec3 at, int tint, @NotNull Transform transform) {
        final var bukkit = (BukkitWorld) world;
        final var display = bukkit.handle().spawn(bukkit.toLocation(at), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setBrightness(FULL_BRIGHT);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTeleportDuration(1);
            d.setItemStack(Items.selection(tint));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(Transforms.toBukkit(transform));
        });
        return new BukkitCursor(display, tint);
    }

    @Override
    public @NotNull TileHandle spawnTile(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull Transform transform) {
        final var bukkit = (BukkitWorld) world;
        final var display = bukkit.handle().spawn(bukkit.toLocation(at), TextDisplay.class, d -> {
            d.text(Component.space());
            d.setBackgroundColor(Color.fromARGB(0xFF000000));
            d.setBrightness(FULL_BRIGHT);
            d.setBillboard(Display.Billboard.FIXED);
            d.setSeeThrough(false);
            d.setShadowed(false);
            d.setPersistent(false);
            d.setInterpolationDuration(1);
            d.setTransformation(Transforms.toBukkit(transform));
        });
        return new BukkitTile(display, transform);
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        final var bukkit = (BukkitWorld) world;
        final var loc = bukkit.toLocation(center);
        final double r2 = radius * radius;
        final var result = new ArrayList<SegmentHandle>();
        for (final var entity : bukkit.handle().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof ItemDisplay display
                    && BukkitSegment.isSegment(display)
                    && display.getLocation().distanceSquared(loc) <= r2) {
                result.add(new BukkitSegment(display));
            }
        }
        return result;
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsByStrokes(@NotNull WorldRef world, @NotNull Set<UUID> strokeIds) {
        if (strokeIds.isEmpty()) return List.of();
        final var result = new ArrayList<SegmentHandle>();
        for (final var display : ((BukkitWorld) world).handle().getEntitiesByClass(ItemDisplay.class)) {
            if (!BukkitSegment.isSegment(display)) continue;
            final var raw = display.getPersistentDataContainer().get(Keys.STROKE_ID, PersistentDataType.STRING);
            if (raw != null && strokeIds.contains(UUID.fromString(raw))) result.add(new BukkitSegment(display));
        }
        return result;
    }

    @Override
    public @NotNull SegmentHandle restore(@NotNull SegmentSnapshot snapshot) {
        final var display = itemDisplay((BukkitWorld) snapshot.world(), snapshot.position(),
                Items.segment(snapshot.rgb()), true, snapshot.transform());
        final var segment = new BukkitSegment(display);
        segment.tag(snapshot.strokeId(), snapshot.segmentId(), snapshot.rgb());
        return segment;
    }

    private static @NotNull ItemDisplay itemDisplay(@NotNull BukkitWorld world, @NotNull Vec3 at,
                                                    @NotNull ItemStack item, boolean persistent, @NotNull Transform transform) {
        return world.handle().spawn(world.toLocation(at), ItemDisplay.class, d -> {
            d.setPersistent(persistent);
            d.setBrightness(FULL_BRIGHT);
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(Transforms.toBukkit(transform));
        });
    }
}
