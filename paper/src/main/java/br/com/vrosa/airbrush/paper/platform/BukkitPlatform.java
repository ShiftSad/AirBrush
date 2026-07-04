package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.item.Items;
import br.com.vrosa.airbrush.paper.item.Keys;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BukkitPlatform implements Platform {

    private static final Display.Brightness FULL_BRIGHT = new Display.Brightness(15, 15);

    @Override
    public @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb, boolean persistent, boolean bright) {
        return new BukkitSegment(itemDisplay((BukkitWorld) world, at, Items.segment(rgb, bright), persistent, Transform.empty(), bright));
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
                Items.segment(snapshot.rgb(), snapshot.bright()), snapshot.persistent(), snapshot.transform(),
                snapshot.bright());
        final var segment = new BukkitSegment(display);
        segment.tag(snapshot.strokeId(), snapshot.segmentId(), snapshot.rgb());
        if (snapshot.anchor() != null) segment.anchor(snapshot.anchor());
        return segment;
    }

    @Override
    public @NotNull List<DropHandle> droppedItemsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        final var bukkit = (BukkitWorld) world;
        final var location = bukkit.toLocation(center);
        final var result = new ArrayList<DropHandle>();
        for (final var entity : bukkit.handle().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Item drop) result.add(new BukkitDrop(drop));
        }
        return result;
    }

    @Override
    public void dropCraftResult(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull String recipeId, double quality,
                                @Nullable CarriedInk ink) {
        final var item = ItemFactory.craftResult(recipeId, quality, ink);
        if (item == null) return;
        final var bukkit = (BukkitWorld) world;
        bukkit.handle().dropItemNaturally(bukkit.toLocation(new Vec3(at.x(), at.y() + 0.5, at.z())), item);
    }

    @Override
    public void craftEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var bukkit = (BukkitWorld) world;
        final var location = bukkit.toLocation(new Vec3(center.x(), center.y() + 0.3, center.z()));
        bukkit.handle().spawnParticle(Particle.WITCH, location, 60, 0.8, 0.4, 0.8, 0.0);
        bukkit.handle().spawnParticle(Particle.END_ROD, location, 20, 0.4, 0.3, 0.4, 0.02);
        bukkit.handle().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 1.0f);
        bukkit.handle().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
    }

    @Override
    public int cauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var state = blockAt(world, block);
        if (state.getType() == Material.CAULDRON) return 0;
        if (state.getType() == Material.WATER_CAULDRON
                && state.getBlockData() instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return -1;
    }

    @Override
    public void setCauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block, int level) {
        final var state = blockAt(world, block);
        if (level <= 0) {
            state.setType(Material.CAULDRON);
            return;
        }
        if (state.getType() != Material.WATER_CAULDRON) state.setType(Material.WATER_CAULDRON);
        final var levelled = (Levelled) state.getBlockData();
        levelled.setLevel(level);
        state.setBlockData(levelled);
    }

    @Override
    public void brewEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var bukkit = (BukkitWorld) world;
        final var location = bukkit.toLocation(center);
        bukkit.handle().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 15, 0.2, 0.1, 0.2, 0.015);
        bukkit.handle().spawnParticle(Particle.WITCH, location, 20, 0.25, 0.15, 0.25, 0.0);
        bukkit.handle().playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 0.9f);
        bukkit.handle().playSound(location, Sound.ENTITY_GENERIC_SPLASH, 0.6f, 1.0f);
    }

    @Override
    public void mixEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var bukkit = (BukkitWorld) world;
        final var location = bukkit.toLocation(center);
        bukkit.handle().spawnParticle(Particle.SPLASH, location, 12, 0.2, 0.1, 0.2, 0.0);
        bukkit.handle().playSound(location, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 1.4f);
    }

    @Override
    public void storeBrew(@NotNull WorldRef world, @NotNull Vec3 block, @Nullable String inkId) {
        final var at = blockAt(world, block);
        final var key = Keys.brew(at.getX(), at.getY(), at.getZ());
        final var pdc = at.getChunk().getPersistentDataContainer();
        if (inkId == null) pdc.remove(key);
        else pdc.set(key, PersistentDataType.STRING, inkId);
    }

    private static @NotNull Block blockAt(@NotNull WorldRef world, @NotNull Vec3 block) {
        return ((BukkitWorld) world).handle().getBlockAt(
                (int) Math.floor(block.x()), (int) Math.floor(block.y()), (int) Math.floor(block.z()));
    }

    private record BukkitDrop(@NotNull Item drop) implements DropHandle {

        @Override
        public @NotNull String itemId() {
            final var stack = drop.getItemStack();
            final var meta = stack.getItemMeta();
            if (meta != null) {
                final var custom = meta.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING);
                if (custom != null) return Keys.NAMESPACE + ":" + custom;
            }
            return stack.getType().getKey().toString();
        }

        @Override
        public int amount() {
            return drop.getItemStack().getAmount();
        }

        @Override
        public @NotNull Vec3 position() {
            return BukkitWorld.toVec3(drop.getLocation());
        }

        @Override
        public void consume(int amount) {
            final var stack = drop.getItemStack();
            if (amount >= stack.getAmount()) {
                drop.remove();
            } else {
                stack.setAmount(stack.getAmount() - amount);
                drop.setItemStack(stack);
            }
        }

        @Override
        public @Nullable String inkId() {
            final var meta = drop.getItemStack().getItemMeta();
            return meta == null ? null : meta.getPersistentDataContainer().get(Keys.INK, PersistentDataType.STRING);
        }

        @Override
        public @Nullable Integer inkColor() {
            final var meta = drop.getItemStack().getItemMeta();
            return meta == null ? null : meta.getPersistentDataContainer().get(Keys.INK_COLOR, PersistentDataType.INTEGER);
        }

        @Override
        public int durabilityRemaining() {
            if (!(drop.getItemStack().getItemMeta() instanceof Damageable meta)) return 0;
            final int max = meta.hasMaxDamage() ? meta.getMaxDamage() : drop.getItemStack().getType().getMaxDurability();
            return Math.max(0, max - meta.getDamage());
        }
    }

    // fullBright: only luminous inks glow; the rest shade with the ambient light.
    private static @NotNull ItemDisplay itemDisplay(@NotNull BukkitWorld world, @NotNull Vec3 at,
                                                    @NotNull ItemStack item, boolean persistent,
                                                    @NotNull Transform transform, boolean fullBright) {
        return world.handle().spawn(world.toLocation(at), ItemDisplay.class, d -> {
            d.setPersistent(persistent);
            if (fullBright) d.setBrightness(FULL_BRIGHT);
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setTransformation(Transforms.toBukkit(transform));
        });
    }
}
