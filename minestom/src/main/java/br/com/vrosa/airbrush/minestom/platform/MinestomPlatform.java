package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.minestom.item.MinestomItems;
import br.com.vrosa.airbrush.minestom.item.Tags;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MinestomPlatform implements Platform {

    @Override
    public @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb, boolean persistent, boolean bright) {
        final var entity = itemDisplay(world, at, MinestomItems.segment(rgb, bright), Transform.empty(), bright);
        entity.setTag(Tags.SEGMENT_BRIGHT, bright);
        return new MinestomSegment(entity);
    }

    @Override
    public @NotNull CursorHandle spawnCursor(@NotNull WorldRef world, @NotNull Vec3 at, int tint, @NotNull Transform transform) {
        final var entity = itemDisplay(world, at, MinestomItems.selection(tint), transform, true);
        ((AbstractDisplayMeta) entity.getEntityMeta()).setPosRotInterpolationDuration(1);
        return new MinestomCursor(entity, tint);
    }

    @Override
    public @NotNull TileHandle spawnTile(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull Transform transform) {
        final var entity = new Entity(EntityType.TEXT_DISPLAY);
        entity.setNoGravity(true);
        final var meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setText(Component.space());
        meta.setBackgroundColor(0xFF000000);
        meta.setUseDefaultBackground(false);
        meta.setSeeThrough(false);
        meta.setShadow(false);
        meta.setBrightness(15, 15);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        meta.setTransformationInterpolationDuration(1);
        meta.setNotifyAboutChanges(true);
        DisplayMetas.apply(meta, transform);

        entity.setInstance(instance(world), pos(world, at));
        return new MinestomTile(entity, transform);
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        final var centerPos = pos(world, center);
        final double r2 = radius * radius;
        final var result = new ArrayList<SegmentHandle>();
        for (final var entity : instance(world).getNearbyEntities(centerPos, radius)) {
            if (MinestomSegment.isSegment(entity) && entity.getPosition().distanceSquared(centerPos) <= r2) {
                result.add(new MinestomSegment(entity));
            }
        }
        return result;
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsByStrokes(@NotNull WorldRef world, @NotNull Set<UUID> strokeIds) {
        if (strokeIds.isEmpty()) return List.of();
        final var result = new ArrayList<SegmentHandle>();
        for (final var entity : instance(world).getEntities()) {
            if (!MinestomSegment.isSegment(entity)) continue;
            final var raw = entity.getTag(Tags.STROKE_ID);
            if (raw != null && strokeIds.contains(UUID.fromString(raw))) result.add(new MinestomSegment(entity));
        }
        return result;
    }

    @Override
    public @NotNull SegmentHandle restore(@NotNull SegmentSnapshot snapshot) {
        final var entity = itemDisplay(snapshot.world(), snapshot.position(),
                MinestomItems.segment(snapshot.rgb(), snapshot.bright()), snapshot.transform(), snapshot.bright());
        entity.setTag(Tags.SEGMENT_BRIGHT, snapshot.bright());
        final var segment = new MinestomSegment(entity);
        segment.tag(snapshot.strokeId(), snapshot.segmentId(), snapshot.rgb());
        if (snapshot.anchor() != null) segment.anchor(snapshot.anchor());
        return segment;
    }

    @Override
    public @NotNull List<DropHandle> droppedItemsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        final var result = new ArrayList<DropHandle>();
        for (final var entity : instance(world).getNearbyEntities(pos(world, center), radius)) {
            if (entity instanceof ItemEntity drop) result.add(new MinestomDrop(drop));
        }
        return result;
    }

    @Override
    public void dropCraftResult(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull String recipeId, double quality,
                                @Nullable CarriedInk ink) {
        final var item = MinestomItems.craftResult(recipeId, quality, ink);
        if (item == null) return;

        final var random = ThreadLocalRandom.current();
        final var drop = new ItemEntity(item);
        drop.setPickupDelay(Duration.ofMillis(500));
        drop.setInstance(instance(world), pos(world, new Vec3(at.x(), at.y() + 0.5, at.z())));
        drop.setVelocity(new Vec(random.nextDouble(-1.0, 1.0), 2.0, random.nextDouble(-1.0, 1.0)));
    }

    @Override
    public void craftEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var instance = instance(world);
        final var at = pos(world, new Vec3(center.x(), center.y() + 0.3, center.z()));
        instance.sendGroupedPacket(new ParticlePacket(Particle.WITCH, at, new Vec(0.8, 0.4, 0.8), 0f, 60));
        instance.sendGroupedPacket(new ParticlePacket(Particle.END_ROD, at, new Vec(0.4, 0.3, 0.4), 0.02f, 20));
        instance.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.BLOCK, 1.5f, 1.0f),
                at.x(), at.y(), at.z());
        instance.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.BLOCK, 0.6f, 1.5f),
                at.x(), at.y(), at.z());
    }

    @Override
    public int cauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var state = instance(world).getBlock(
                (int) Math.floor(block.x()), (int) Math.floor(block.y()), (int) Math.floor(block.z()));
        if (state.compare(Block.CAULDRON)) return 0;
        if (state.compare(Block.WATER_CAULDRON)) {
            final var level = state.getProperty("level");
            return level == null ? -1 : Integer.parseInt(level);
        }
        return -1;
    }

    @Override
    public void setCauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block, int level) {
        instance(world).setBlock(
                (int) Math.floor(block.x()), (int) Math.floor(block.y()), (int) Math.floor(block.z()),
                level <= 0 ? Block.CAULDRON : Block.WATER_CAULDRON.withProperty("level", String.valueOf(level)));
    }

    @Override
    public void brewEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var instance = instance(world);
        final var at = pos(world, center);
        instance.sendGroupedPacket(new ParticlePacket(Particle.CAMPFIRE_COSY_SMOKE, at, new Vec(0.2, 0.1, 0.2), 0.015f, 15));
        instance.sendGroupedPacket(new ParticlePacket(Particle.WITCH, at, new Vec(0.25, 0.15, 0.25), 0f, 20));
        instance.playSound(Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1.0f, 0.9f),
                at.x(), at.y(), at.z());
        instance.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_SPLASH, Sound.Source.BLOCK, 0.6f, 1.0f),
                at.x(), at.y(), at.z());
    }

    @Override
    public void mixEffects(@NotNull WorldRef world, @NotNull Vec3 center) {
        final var instance = instance(world);
        final var at = pos(world, center);
        instance.sendGroupedPacket(new ParticlePacket(Particle.SPLASH, at, new Vec(0.2, 0.1, 0.2), 0f, 12));
        instance.playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_SPLASH, Sound.Source.BLOCK, 0.5f, 1.4f),
                at.x(), at.y(), at.z());
    }

    @Override
    public void storeBrew(@NotNull WorldRef world, @NotNull Vec3 block, @Nullable String inkId) {
        // The Minestom test server doesn't persist the world, so there's nothing to save.
    }

    private record MinestomDrop(@NotNull ItemEntity drop) implements DropHandle {

        @Override
        public @NotNull String itemId() {
            final var stack = drop.getItemStack();
            final var custom = stack.getTag(Tags.ITEM_TYPE);
            if (custom != null) return Tags.NAMESPACE + ":" + custom;
            return stack.material().key().asString();
        }

        @Override
        public int amount() {
            return drop.getItemStack().amount();
        }

        @Override
        public @NotNull Vec3 position() {
            final var position = drop.getPosition();
            return new Vec3(position.x(), position.y(), position.z());
        }

        @Override
        public void consume(int amount) {
            final var stack = drop.getItemStack();
            if (amount >= stack.amount()) drop.remove();
            else drop.setItemStack(stack.withAmount(stack.amount() - amount));
        }

        @Override
        public @Nullable String inkId() {
            return drop.getItemStack().getTag(Tags.INK);
        }

        @Override
        public @Nullable Integer inkColor() {
            return drop.getItemStack().getTag(Tags.INK_COLOR);
        }

        @Override
        public int durabilityRemaining() {
            final var stack = drop.getItemStack();
            final int max = stack.get(DataComponents.MAX_DAMAGE, 0);
            if (max <= 0) return 0;
            return Math.max(0, max - stack.get(DataComponents.DAMAGE, 0));
        }
    }

    // fullBright: only luminous inks glow; the rest shade with the ambient light.
    private static @NotNull Entity itemDisplay(@NotNull WorldRef world, @NotNull Vec3 at,
                                               @NotNull ItemStack item, @NotNull Transform transform, boolean fullBright) {
        final var entity = new Entity(EntityType.ITEM_DISPLAY);
        entity.setNoGravity(true);
        final var meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setItemStack(item);
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.NONE);
        if (fullBright) meta.setBrightness(15, 15);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        meta.setNotifyAboutChanges(true);
        DisplayMetas.apply(meta, transform);

        entity.setInstance(instance(world), pos(world, at));
        return entity;
    }

    private static @NotNull Instance instance(@NotNull WorldRef ref) {
        return ((MinestomWorld) ref).handle();
    }

    private static @NotNull Pos pos(@NotNull WorldRef ref, @NotNull Vec3 at) {
        return ((MinestomWorld) ref).toPos(at);
    }
}
