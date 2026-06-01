package br.com.vrosa.witchcraft.minestom.platform;

import br.com.vrosa.witchcraft.platform.TileHandle;
import br.com.vrosa.witchcraft.platform.Transform;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.jetbrains.annotations.NotNull;

public final class MinestomTile implements TileHandle {

    private final Entity entity;
    private Transform transform;

    public MinestomTile(@NotNull Entity entity, @NotNull Transform transform) {
        this.entity = entity;
        this.transform = transform;
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
    public void setBackgroundColor(int argb) {
        ((TextDisplayMeta) entity.getEntityMeta()).setBackgroundColor(argb);
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        this.transform = transform;
        DisplayMetas.apply((AbstractDisplayMeta) entity.getEntityMeta(), transform);
    }

    @Override
    public @NotNull Transform transform() {
        return transform;
    }

    @Override
    public void setInterpolationDelay(int delay) {
        ((AbstractDisplayMeta) entity.getEntityMeta()).setTransformationInterpolationStartDelta(delay);
    }
}
