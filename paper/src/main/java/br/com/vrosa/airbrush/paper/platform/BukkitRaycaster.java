package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.platform.AbstractRaycaster;
import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import io.papermc.paper.raytracing.BlockCollisionMode;
import io.papermc.paper.raytracing.RayTraceTarget;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Particle;
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

        final var trace = world.rayTrace(builder -> {
            builder.blockCollisionMode(BlockCollisionMode.OUTLINE);
            builder.fluidCollisionMode(FluidCollisionMode.ALWAYS);
            builder.direction(eye.getDirection());
            builder.maxDistance(maxDistance());
            builder.targets(RayTraceTarget.BLOCK);
            builder.start(eye);
        });
        if (trace == null) return null;

        final var hit = trace.getHitPosition();
        final var face = trace.getHitBlockFace();
        final Vector normal = face != null ? face.getDirection() : eye.getDirection().multiply(-1);
        return new Pose(
                new BukkitWorld(world),
                new Vec3(hit.getX(), hit.getY(), hit.getZ()),
                new Vector3f((float) normal.getX(), (float) normal.getY(), (float) normal.getZ()));
    }

    @Override
    protected void showCursor(@NotNull WPlayer player, @NotNull Pose hit) {
        final var world = (BukkitWorld) hit.world();
        world.handle().spawnParticle(Particle.OMINOUS_SPAWNING, world.toLocation(hit.position()), 0);
    }
}
