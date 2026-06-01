package br.com.vrosa.witchcraft.minestom.platform;

import br.com.vrosa.witchcraft.minestom.item.MinestomItems;
import br.com.vrosa.witchcraft.minestom.item.Tags;
import br.com.vrosa.witchcraft.platform.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MinestomPlatform implements Platform {

    @Override
    public @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb, boolean persistent) {
        return new MinestomSegment(itemDisplay(world, at, MinestomItems.segment(rgb), Transform.empty()));
    }

    @Override
    public @NotNull CursorHandle spawnCursor(@NotNull WorldRef world, @NotNull Vec3 at, int tint, @NotNull Transform transform) {
        final var entity = itemDisplay(world, at, MinestomItems.selection(tint), transform);
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
                MinestomItems.segment(snapshot.rgb()), snapshot.transform());
        final var segment = new MinestomSegment(entity);
        segment.tag(snapshot.strokeId(), snapshot.segmentId(), snapshot.rgb());
        return segment;
    }

    private static @NotNull Entity itemDisplay(@NotNull WorldRef world, @NotNull Vec3 at,
                                               @NotNull ItemStack item, @NotNull Transform transform) {
        final var entity = new Entity(EntityType.ITEM_DISPLAY);
        entity.setNoGravity(true);
        final var meta = (ItemDisplayMeta) entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setItemStack(item);
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.NONE);
        meta.setBrightness(15, 15);
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
