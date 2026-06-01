package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record Transform(
        @NotNull Vector3f translation,
        @NotNull Quaternionf leftRotation,
        @NotNull Vector3f scale,
        @NotNull Quaternionf rightRotation) {

    public static @NotNull Transform empty() {
        return new Transform(new Vector3f(), new Quaternionf(), new Vector3f(0f), new Quaternionf());
    }

    public static float @NotNull [] toArray(@NotNull Quaternionf q) {
        return new float[]{q.x, q.y, q.z, q.w};
    }
}
