package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.minestom.item.Tags;
import br.com.vrosa.airbrush.platform.*;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MinestomSegment implements SegmentHandle {

    private final Entity entity;

    public MinestomSegment(@NotNull Entity entity) {
        this.entity = entity;
    }

    public @NotNull Entity handle() {
        return entity;
    }

    public static boolean isSegment(@NotNull Entity entity) {
        return entity.getEntityType() == EntityType.ITEM_DISPLAY && entity.hasTag(Tags.STROKE_ID);
    }

    @Override
    public boolean isValid() {
        return !entity.isRemoved();
    }

    @Override
    public void remove() {
        entity.remove();
    }

    @Override
    public void setPersistent(boolean persistent) {
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        DisplayMetas.apply((AbstractDisplayMeta) entity.getEntityMeta(), transform);
    }

    @Override
    public @NotNull WorldRef world() {
        return new MinestomWorld(entity.getInstance());
    }

    @Override
    public @NotNull Vec3 position() {
        return MinestomWorld.toVec3(entity.getPosition());
    }

    @Override
    public void tag(@NotNull UUID strokeId, @NotNull UUID segmentId, int rgb) {
        entity.setTag(Tags.STROKE_ID, strokeId.toString());
        entity.setTag(Tags.SEGMENT_ID, segmentId.toString());
        entity.setTag(Tags.SEGMENT_COLOR, rgb & 0xFFFFFF);
    }

    @Override
    public @Nullable UUID strokeId() {
        final var raw = entity.getTag(Tags.STROKE_ID);
        return raw == null ? null : UUID.fromString(raw);
    }

    @Override
    public @Nullable UUID segmentId() {
        final var raw = entity.getTag(Tags.SEGMENT_ID);
        return raw == null ? null : UUID.fromString(raw);
    }

    @Override
    public int color() {
        final var raw = entity.getTag(Tags.SEGMENT_COLOR);
        return raw == null ? 0xFFFFFF : raw;
    }

    @Override
    public @NotNull SegmentSnapshot snapshot() {
        final var stroke = strokeId();
        final var segment = segmentId();
        return new SegmentSnapshot(
                world(),
                position(),
                DisplayMetas.read((AbstractDisplayMeta) entity.getEntityMeta()),
                stroke == null ? UUID.randomUUID() : stroke,
                segment == null ? UUID.randomUUID() : segment,
                color());
    }
}
