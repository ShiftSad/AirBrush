package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record Vec3(double x, double y, double z) {

    public @NotNull Vec3 lerp(@NotNull Vec3 other, double t) {
        return new Vec3(x + (other.x - x) * t, y + (other.y - y) * t, z + (other.z - z) * t);
    }

    public double distanceSquared(@NotNull Vec3 other) {
        final double dx = x - other.x;
        final double dy = y - other.y;
        final double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public @NotNull Vector3f toVector3f() {
        return new Vector3f((float) x, (float) y, (float) z);
    }
}
