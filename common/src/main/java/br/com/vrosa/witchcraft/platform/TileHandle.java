package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;

public interface TileHandle {

    boolean isValid();

    void remove();

    void setBackgroundColor(int argb);

    void setTransform(@NotNull Transform transform);

    @NotNull Transform transform();

    void setInterpolationDelay(int delay);
}
