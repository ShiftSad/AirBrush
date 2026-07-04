package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HammerListener implements Listener {

    private static final double REACH = 3.0;
    private static final double RAY_SIZE = 0.2;
    private static final int DEBOUNCE_TICKS = 2;

    private final AirBrushEngine engine;
    private final Map<UUID, Integer> lastTransform = new HashMap<>();

    public HammerListener(@NotNull AirBrushEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onPunch(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        final var action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        final var player = event.getPlayer();
        final var hammer = player.getInventory().getItemInMainHand();
        if (!ItemFactory.isHammer(hammer)) return;

        final int tick = Bukkit.getCurrentTick();
        final boolean debounced = tick - lastTransform.getOrDefault(player.getUniqueId(), -DEBOUNCE_TICKS) < DEBOUNCE_TICKS;

        final var wp = BukkitPlayer.of(player);
        final var pointer = engine.raycaster().cast(wp);
        if (pointer != null && !debounced && engine.glyphCraft().attempt(pointer)) {
            event.setCancelled(true);
            lastTransform.put(player.getUniqueId(), tick);
            wp.actionBar(Component.text(
                    Messages.get(player.locale(), Messages.Key.GLYPH_CRAFT_SUCCESS), NamedTextColor.GREEN));
            return;
        }

        final var eye = player.getEyeLocation();
        final var hit = player.getWorld().rayTraceEntities(eye, eye.getDirection(), REACH, RAY_SIZE,
                entity -> entity instanceof Item drop && drop.getItemStack().getType() == Material.AMETHYST_SHARD);
        if (hit == null || !(hit.getHitEntity() instanceof Item drop)) return;

        event.setCancelled(true);
        if (debounced) return;

        if (transmute(player, hammer, drop)) lastTransform.put(player.getUniqueId(), tick);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        lastTransform.remove(event.getPlayer().getUniqueId());
    }

    private static boolean transmute(@NotNull Player player, @NotNull ItemStack hammer, @NotNull Item drop) {
        final var stack = drop.getItemStack();
        final int requested = player.isSneaking() ? stack.getAmount() : 1;
        final int amount = Math.min(requested, remainingUses(hammer));
        if (amount <= 0) return false;

        final var location = drop.getLocation();
        if (amount >= stack.getAmount()) {
            drop.remove();
        } else {
            stack.setAmount(stack.getAmount() - amount);
            drop.setItemStack(stack);
        }

        final var world = location.getWorld();
        for (int i = 0; i < amount; i++) {
            world.dropItemNaturally(location, ItemFactory.amethystDye());
        }

        player.damageItemStack(EquipmentSlot.HAND, amount);

        world.spawnParticle(Particle.WITCH, location.clone().add(0, 0.25, 0),
                Math.min(48, 8 + 2 * amount), 0.2, 0.2, 0.2, 0.0);
        world.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 3.0f, 1.2f);
        return true;
    }

    private static int remainingUses(@NotNull ItemStack hammer) {
        if (!(hammer.getItemMeta() instanceof Damageable meta)) return 0;
        final int max = meta.hasMaxDamage() ? meta.getMaxDamage() : hammer.getType().getMaxDurability();
        return Math.max(0, max - meta.getDamage());
    }
}
