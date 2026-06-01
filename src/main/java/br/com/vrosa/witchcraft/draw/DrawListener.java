package br.com.vrosa.witchcraft.draw;

import br.com.vrosa.witchcraft.keys.ItemDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public final class DrawListener implements Listener {

    private final DrawService service;

    public DrawListener(@NotNull DrawService service) {
        this.service = service;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        final var player = event.getPlayer();
        final var item = event.getItem();

        if (item == null || ItemDefinition.getType(item) != ItemDefinition.PENCIL) return;

        final var action = event.getAction();
        final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        service.handlePencil(player, right);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        service.remove(event.getPlayer());
    }
}
