package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.paper.item.Items;
import br.com.vrosa.airbrush.platform.CursorHandle;
import br.com.vrosa.airbrush.platform.Transform;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.bukkit.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;

public final class BukkitCursor implements CursorHandle {

    private final ItemDisplay display;
    private int tint;

    public BukkitCursor(@NotNull ItemDisplay display, int tint) {
        this.display = display;
        this.tint = tint;
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
    public @NotNull WorldRef world() {
        return new BukkitWorld(display.getWorld());
    }

    @Override
    public void moveTo(@NotNull Vec3 position) {
        display.teleport(new BukkitWorld(display.getWorld()).toLocation(position));
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        display.setTransformation(Transforms.toBukkit(transform));
    }

    @Override
    public int tint() {
        return tint;
    }

    @Override
    public void setTint(int tint) {
        this.tint = tint;
        display.setItemStack(Items.selection(tint));
    }
}
