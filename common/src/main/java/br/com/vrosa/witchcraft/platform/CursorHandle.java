package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;

public interface CursorHandle {

    boolean isValid();

    void remove();

    @NotNull WorldRef world();

    void moveTo(@NotNull Vec3 position);

    void setTransform(@NotNull Transform transform);

    int tint();

    void setTint(int tint);
}
