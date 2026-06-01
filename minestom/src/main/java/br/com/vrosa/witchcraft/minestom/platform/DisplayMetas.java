package br.com.vrosa.witchcraft.minestom.platform;

import br.com.vrosa.witchcraft.platform.Transform;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class DisplayMetas {

    private DisplayMetas() {}

    static void apply(@NotNull AbstractDisplayMeta meta, @NotNull Transform transform) {
        meta.setNotifyAboutChanges(false);
        meta.setTranslation(new Vec(
                transform.translation().x(), transform.translation().y(), transform.translation().z()));
        meta.setScale(new Vec(
                transform.scale().x(), transform.scale().y(), transform.scale().z()));
        meta.setLeftRotation(Transform.toArray(transform.leftRotation()));
        meta.setRightRotation(Transform.toArray(transform.rightRotation()));
        meta.setNotifyAboutChanges(true);
    }

    static @NotNull Transform read(@NotNull AbstractDisplayMeta meta) {
        final var t = meta.getTranslation();
        final var s = meta.getScale();
        final float[] l = meta.getLeftRotation();
        final float[] r = meta.getRightRotation();
        return new Transform(
                new Vector3f((float) t.x(), (float) t.y(), (float) t.z()),
                new Quaternionf(l[0], l[1], l[2], l[3]),
                new Vector3f((float) s.x(), (float) s.y(), (float) s.z()),
                new Quaternionf(r[0], r[1], r[2], r[3]));
    }
}
