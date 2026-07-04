package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.platform.AbstractRaycaster;
import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import br.com.vrosa.airbrush.platform.WorldRef;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.DoubleSupplier;

public final class MinestomRaycaster extends AbstractRaycaster {

    public MinestomRaycaster(@NotNull DoubleSupplier maxDistance) {
        super(maxDistance);
    }

    @Override
    protected @Nullable Pose trace(@NotNull WPlayer player) {
        final var handle = ((MinestomPlayer) player).handle();
        final var instance = handle.getInstance();
        if (instance == null) return null;

        final var eye = handle.getPosition().add(0, handle.getEyeHeight(), 0);
        final var dir = handle.getPosition().direction();
        return dda(instance, eye.x(), eye.y(), eye.z(), dir.x(), dir.y(), dir.z(), maxDistance(player));
    }

    @Override
    public @Nullable Pose project(@NotNull WorldRef world, @NotNull Vec3 origin, @NotNull Vector3f direction,
                                  double maxDistance) {
        return dda(((MinestomWorld) world).handle(),
                origin.x(), origin.y(), origin.z(), direction.x, direction.y, direction.z, maxDistance);
    }

    private static @Nullable Pose dda(@NotNull Instance instance,
                                      double ox, double oy, double oz,
                                      double dx, double dy, double dz, double maxDistance) {
        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);

        // Match Paper's rayTraceBlocks: a ray starting inside a solid block hits immediately.
        if (!instance.getBlock(x, y, z).isAir()) {
            final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            final var normal = length < 1.0e-9
                    ? new Vector3f(0f, 1f, 0f)
                    : new Vector3f((float) (-dx / length), (float) (-dy / length), (float) (-dz / length));
            return new Pose(new MinestomWorld(instance), new Vec3(ox, oy, oz), normal);
        }

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

        while (t <= maxDistance) {
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
            if (t > maxDistance) break;

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
