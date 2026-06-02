package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public record MinestomWorld(@NotNull Instance handle) implements WorldRef {

    public static @NotNull MinestomWorld of(@NotNull Instance instance) {
        return new MinestomWorld(instance);
    }

    public @NotNull Pos toPos(@NotNull Vec3 position) {
        return new Pos(position.x(), position.y(), position.z());
    }

    public static @NotNull Vec3 toVec3(@NotNull Point point) {
        return new Vec3(point.x(), point.y(), point.z());
    }
}
