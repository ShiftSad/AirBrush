package br.com.vrosa.witchcraft.paper.platform;

import br.com.vrosa.witchcraft.paper.item.Keys;
import br.com.vrosa.witchcraft.platform.SegmentHandle;
import br.com.vrosa.witchcraft.platform.SegmentSnapshot;
import br.com.vrosa.witchcraft.platform.Transform;
import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WorldRef;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BukkitSegment implements SegmentHandle {

    private final ItemDisplay display;

    public BukkitSegment(@NotNull ItemDisplay display) {
        this.display = display;
    }

    public @NotNull ItemDisplay handle() {
        return display;
    }

    public static boolean isSegment(@NotNull ItemDisplay display) {
        return display.getPersistentDataContainer().has(Keys.STROKE_ID, PersistentDataType.STRING);
    }

    @Override
    public boolean isValid() {
        return display.isValid();
    }

    @Override
    public void remove() {
        display.remove();
    }

    @Override
    public void setPersistent(boolean persistent) {
        display.setPersistent(persistent);
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        display.setTransformation(Transforms.toBukkit(transform));
    }

    @Override
    public @NotNull WorldRef world() {
        return new BukkitWorld(display.getWorld());
    }

    @Override
    public @NotNull Vec3 position() {
        return BukkitWorld.toVec3(display.getLocation());
    }

    @Override
    public void tag(@NotNull UUID strokeId, @NotNull UUID segmentId, int rgb) {
        final var pdc = display.getPersistentDataContainer();
        pdc.set(Keys.STROKE_ID, PersistentDataType.STRING, strokeId.toString());
        pdc.set(Keys.SEGMENT_ID, PersistentDataType.STRING, segmentId.toString());
        pdc.set(Keys.SEGMENT_COLOR, PersistentDataType.INTEGER, rgb & 0xFFFFFF);
    }

    @Override
    public @Nullable UUID strokeId() {
        final var raw = display.getPersistentDataContainer().get(Keys.STROKE_ID, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    @Override
    public @Nullable UUID segmentId() {
        final var raw = display.getPersistentDataContainer().get(Keys.SEGMENT_ID, PersistentDataType.STRING);
        return raw == null ? null : UUID.fromString(raw);
    }

    @Override
    public int color() {
        final var raw = display.getPersistentDataContainer().get(Keys.SEGMENT_COLOR, PersistentDataType.INTEGER);
        return raw == null ? 0xFFFFFF : raw;
    }

    @Override
    public @NotNull SegmentSnapshot snapshot() {
        final var stroke = strokeId();
        final var segment = segmentId();
        return new SegmentSnapshot(
                world(),
                position(),
                Transforms.fromBukkit(display.getTransformation()),
                stroke == null ? UUID.randomUUID() : stroke,
                segment == null ? UUID.randomUUID() : segment,
                color());
    }
}
