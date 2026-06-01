package br.com.vrosa.witchcraft.raycast;

import br.com.vrosa.witchcraft.keys.ItemDefinition;
import io.papermc.paper.raytracing.BlockCollisionMode;
import io.papermc.paper.raytracing.RayTraceTarget;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.WeakHashMap;

public final class Raycaster {

    private static final double MAX_DISTANCE = 10;

    private final WeakHashMap<Player, RayHit> cache = new WeakHashMap<>();

    public @Nullable RayHit current(@NotNull Player player) {
        return cache.get(player);
    }

    public void clear(@NotNull Player player) {
        cache.remove(player);
    }

    public void tick(@NotNull Player player, boolean showCursor) {
        if (!ItemDefinition.hasAnyType(player.getInventory().getItemInMainHand())) {
            cache.remove(player);
            return;
        }

        final var eye = player.getEyeLocation();
        final var world = player.getWorld();

        final var trace = world.rayTrace(builder -> {
            builder.blockCollisionMode(BlockCollisionMode.OUTLINE);
            builder.fluidCollisionMode(FluidCollisionMode.ALWAYS);
            builder.direction(eye.getDirection());
            builder.maxDistance(MAX_DISTANCE);
            builder.targets(RayTraceTarget.BLOCK);
            builder.start(eye);
        });

        if (trace == null) {
            cache.remove(player);
            return;
        }

        final var position = trace.getHitPosition().toLocation(world);
        if (showCursor) world.spawnParticle(Particle.OMINOUS_SPAWNING, position, 0);

        final var face = trace.getHitBlockFace();
        final Vector normal = face != null ? face.getDirection() : eye.getDirection().multiply(-1);
        cache.put(player, new RayHit(position,
                new Vector3f((float) normal.getX(), (float) normal.getY(), (float) normal.getZ())));
    }
}
