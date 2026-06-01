package br.com.vrosa.witchcraft.minestom.platform;

import br.com.vrosa.witchcraft.platform.AbstractRaycaster;
import br.com.vrosa.witchcraft.platform.Pose;
import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class MinestomRaycaster extends AbstractRaycaster {

    @Override
    protected @Nullable Pose trace(@NotNull WPlayer player) {
        final var handle = ((MinestomPlayer) player).handle();
        final var instance = handle.getInstance();
        if (instance == null) return null;

        final var eye = handle.getPosition().add(0, handle.getEyeHeight(), 0);
        final var dir = handle.getPosition().direction();

        final double ox = eye.x();
        final double oy = eye.y();
        final double oz = eye.z();
        final double dx = dir.x();
        final double dy = dir.y();
        final double dz = dir.z();

        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);

        final int stepX = dx > 0 ? 1 : -1;
        final int stepY = dy > 0 ? 1 : -1;
        final int stepZ = dz > 0 ? 1 : -1;

        final double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        final double tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        final double tDeltaZ = dz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dz);

        double tMaxX = dx == 0 ? Double.MAX_VALUE : (stepX > 0 ? (x + 1 - ox) : (ox - x)) * tDeltaX;
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (stepY > 0 ? (y + 1 - oy) : (oy - y)) * tDeltaY;
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : (stepZ > 0 ? (z + 1 - oz) : (oz - z)) * tDeltaZ;

        double t = 0;
        int axis;

        while (t <= MAX_DISTANCE) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                axis = 0;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                axis = 1;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                axis = 2;
            }
            if (t > MAX_DISTANCE) break;

            if (!instance.getBlock(x, y, z).isAir()) {
                final var normal = switch (axis) {
                    case 0 -> new Vector3f(-stepX, 0, 0);
                    case 1 -> new Vector3f(0, -stepY, 0);
                    default -> new Vector3f(0, 0, -stepZ);
                };
                return new Pose(new MinestomWorld(instance),
                        new Vec3(ox + dx * t, oy + dy * t, oz + dz * t), normal);
            }
        }
        return null;
    }

    @Override
    protected void showCursor(@NotNull WPlayer player, @NotNull Pose hit) {
    }
}
