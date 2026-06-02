package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.minestom.item.MinestomItems;
import br.com.vrosa.airbrush.platform.CursorHandle;
import br.com.vrosa.airbrush.platform.Transform;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import org.jetbrains.annotations.NotNull;

public final class MinestomCursor implements CursorHandle {

    private final Entity entity;
    private int tint;

    public MinestomCursor(@NotNull Entity entity, int tint) {
        this.entity = entity;
        this.tint = tint;
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
    public @NotNull WorldRef world() {
        return new MinestomWorld(entity.getInstance());
    }

    @Override
    public void moveTo(@NotNull Vec3 position) {
        entity.teleport(new MinestomWorld(entity.getInstance()).toPos(position));
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        DisplayMetas.apply((AbstractDisplayMeta) entity.getEntityMeta(), transform);
    }

    @Override
    public int tint() {
        return tint;
    }

    @Override
    public void setTint(int tint) {
        this.tint = tint;
        ((ItemDisplayMeta) entity.getEntityMeta()).setItemStack(MinestomItems.selection(tint));
    }
}
