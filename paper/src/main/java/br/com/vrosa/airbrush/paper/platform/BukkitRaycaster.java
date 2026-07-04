package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.platform.AbstractRaycaster;
import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.DoubleSupplier;

public final class BukkitRaycaster extends AbstractRaycaster {

    public BukkitRaycaster(@NotNull DoubleSupplier maxDistance) {
        super(maxDistance);
    }

    @Override
    protected @Nullable Pose trace(@NotNull WPlayer player) {
        final var handle = ((BukkitPlayer) player).handle();
        final var eye = handle.getEyeLocation();
        final var world = handle.getWorld();

        final var trace = world.rayTraceBlocks(
                eye, eye.getDirection(), maxDistance(player), FluidCollisionMode.ALWAYS, false);
        return toPose(new BukkitWorld(world), trace, eye.getDirection());
    }

    @Override
    public @Nullable Pose project(@NotNull WorldRef world, @NotNull Vec3 origin, @NotNull Vector3f direction,
                                  double maxDistance) {
        final var bukkit = (BukkitWorld) world;
        final var dir = new Vector(direction.x, direction.y, direction.z);
        final var trace = bukkit.handle().rayTraceBlocks(
                bukkit.toLocation(origin), dir, maxDistance, FluidCollisionMode.ALWAYS, false);
        return toPose(bukkit, trace, dir);
    }

    private static @Nullable Pose toPose(@NotNull BukkitWorld world, @Nullable RayTraceResult trace,
                                         @NotNull Vector direction) {
        if (trace == null) return null;
        final var hit = trace.getHitPosition();
        final var face = trace.getHitBlockFace();
        final Vector normal = face != null ? face.getDirection() : direction.clone().multiply(-1);
        return new Pose(
                world,
                new Vec3(hit.getX(), hit.getY(), hit.getZ()),
                new Vector3f((float) normal.getX(), (float) normal.getY(), (float) normal.getZ()));
    }

    @Override
    protected void showCursor(@NotNull WPlayer player, @NotNull Pose hit) {
        final var world = (BukkitWorld) hit.world();
        world.handle().spawnParticle(Particle.OMINOUS_SPAWNING, world.toLocation(hit.position()), 0);
    }
}
