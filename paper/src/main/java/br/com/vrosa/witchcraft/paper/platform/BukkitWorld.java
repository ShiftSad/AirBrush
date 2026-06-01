package br.com.vrosa.witchcraft.paper.platform;

import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WorldRef;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public record BukkitWorld(@NotNull World handle) implements WorldRef {

    public static @NotNull BukkitWorld of(@NotNull World world) {
        return new BukkitWorld(world);
    }

    public @NotNull Location toLocation(@NotNull Vec3 position) {
        return new Location(handle, position.x(), position.y(), position.z());
    }

    public static @NotNull Vec3 toVec3(@NotNull Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }
}
