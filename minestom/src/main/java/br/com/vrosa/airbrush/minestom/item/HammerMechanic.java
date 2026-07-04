package br.com.vrosa.airbrush.minestom.item;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class HammerMechanic {

    private static final double REACH = 3.0;
    private static final double HIT_MARGIN = 0.15;
    private static final long CANCEL_WINDOW_MS = 100;

    private static final Map<UUID, Long> lastTransform = new ConcurrentHashMap<>();

    private HammerMechanic() {}

    public static boolean punch(@NotNull AirBrushEngine engine, @NotNull Player player) {
        if (!MinestomItems.isHammer(player.getItemInMainHand())) return false;

        final var instance = player.getInstance();
        if (instance == null) return false;

        final var wp = MinestomPlayer.of(player);
        final var pointer = engine.raycaster().cast(wp);
        if (pointer != null && engine.glyphCraft().attempt(pointer)) {
            lastTransform.put(player.getUuid(), System.currentTimeMillis());
            wp.actionBar(Component.text(
                    Messages.get(wp.locale(), Messages.Key.GLYPH_CRAFT_SUCCESS), NamedTextColor.GREEN));
            return true;
        }

        final var drop = findTarget(player, instance);
        if (drop == null) return false;

        lastTransform.put(player.getUuid(), System.currentTimeMillis());
        transmute(player, instance, drop);
        return true;
    }

    public static void handleBlockBreak(@NotNull Player player, @NotNull Block block) {
        if (!MinestomItems.isHammer(player.getItemInMainHand())) return;
        if (block.registry().hardness() <= 0) return;
        MinestomPlayer.of(player).damageHeldItem(1);
    }

    public static boolean justTransformed(@NotNull Player player) {
        final var last = lastTransform.get(player.getUuid());
        return last != null && System.currentTimeMillis() - last <= CANCEL_WINDOW_MS;
    }

    public static void forget(@NotNull Player player) {
        lastTransform.remove(player.getUuid());
    }

    private static @Nullable ItemEntity findTarget(@NotNull Player player, @NotNull Instance instance) {
        final var position = player.getPosition();
        final var eye = new Vec(position.x(), position.y() + player.getEyeHeight(), position.z());
        final var direction = position.direction();

        ItemEntity nearest = null;
        double nearestDistance = REACH;
        for (final var entity : instance.getNearbyEntities(position, REACH + 1)) {
            if (!(entity instanceof ItemEntity drop)) continue;
            if (drop.getItemStack().material() != Material.AMETHYST_SHARD) continue;

            final var box = drop.getBoundingBox().growSymmetrically(HIT_MARGIN, HIT_MARGIN, HIT_MARGIN);
            if (!box.boundingBoxRayIntersectionCheck(eye, direction, drop.getPosition())) continue;

            final double distance = drop.getPosition().distance(eye);
            if (distance < nearestDistance) {
                nearest = drop;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void transmute(@NotNull Player player, @NotNull Instance instance, @NotNull ItemEntity drop) {
        final var hammer = player.getItemInMainHand();
        final var stack = drop.getItemStack();
        final int requested = player.isSneaking() ? stack.amount() : 1;
        final int amount = Math.min(requested, remainingUses(hammer));
        if (amount <= 0) return;

        final var position = drop.getPosition();
        if (amount >= stack.amount()) drop.remove();
        else drop.setItemStack(stack.withAmount(stack.amount() - amount));

        final var random = ThreadLocalRandom.current();
        for (int i = 0; i < amount; i++) {
            final var dye = new ItemEntity(MinestomItems.amethystDye());
            dye.setPickupDelay(Duration.ofMillis(500));
            dye.setMergeable(false);
            dye.setInstance(instance, position);
            dye.setVelocity(new Vec(random.nextDouble(-1.5, 1.5), 2.5, random.nextDouble(-1.5, 1.5)));
        }

        MinestomPlayer.of(player).damageHeldItem(amount);

        instance.sendGroupedPacket(new ParticlePacket(Particle.WITCH, position.add(0, 0.25, 0),
                new Vec(0.2, 0.2, 0.2), 0f, Math.min(48, 8 + 2 * amount)));
        instance.playSound(Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_CHIME, Sound.Source.BLOCK, 3f, 1.2f),
                position.x(), position.y(), position.z());
    }

    private static int remainingUses(@NotNull ItemStack hammer) {
        return Math.max(0, hammer.get(DataComponents.MAX_DAMAGE, 0) - hammer.get(DataComponents.DAMAGE, 0));
    }
}
