package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.platform.Transform;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Transforms {

    private Transforms() {}

    static @NotNull Transformation toBukkit(@NotNull Transform t) {
        return new Transformation(
                new Vector3f(t.translation()),
                new Quaternionf(t.leftRotation()),
                new Vector3f(t.scale()),
                new Quaternionf(t.rightRotation()));
    }

    static @NotNull Transform fromBukkit(@NotNull Transformation t) {
        return new Transform(
                new Vector3f(t.getTranslation()),
                new Quaternionf(t.getLeftRotation()),
                new Vector3f(t.getScale()),
                new Quaternionf(t.getRightRotation()));
    }
}
