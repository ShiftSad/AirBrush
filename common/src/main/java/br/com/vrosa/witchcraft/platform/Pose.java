package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record Pose(@NotNull WorldRef world, @NotNull Vec3 position, @NotNull Vector3f normal) {

    public @NotNull Pose copy() {
        return new Pose(world, position, new Vector3f(normal));
    }

    public boolean sameWorld(@NotNull Pose other) {
        return world.equals(other.world);
    }
}
