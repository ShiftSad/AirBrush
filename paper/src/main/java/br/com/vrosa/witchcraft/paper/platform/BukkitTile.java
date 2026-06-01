package br.com.vrosa.witchcraft.paper.platform;

import br.com.vrosa.witchcraft.platform.TileHandle;
import br.com.vrosa.witchcraft.platform.Transform;
import org.bukkit.Color;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

public final class BukkitTile implements TileHandle {

    private final TextDisplay display;
    private Transform transform;

    public BukkitTile(@NotNull TextDisplay display, @NotNull Transform transform) {
        this.display = display;
        this.transform = transform;
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
    public void setBackgroundColor(int argb) {
        display.setBackgroundColor(Color.fromARGB(argb));
    }

    @Override
    public void setTransform(@NotNull Transform transform) {
        this.transform = transform;
        display.setTransformation(Transforms.toBukkit(transform));
    }

    @Override
    public @NotNull Transform transform() {
        return transform;
    }

    @Override
    public void setInterpolationDelay(int delay) {
        display.setInterpolationDelay(delay);
    }
}
