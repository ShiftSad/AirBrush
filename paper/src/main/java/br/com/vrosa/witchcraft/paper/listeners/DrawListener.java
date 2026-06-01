package br.com.vrosa.witchcraft.paper.listeners;

import br.com.vrosa.witchcraft.core.draw.DrawService;
import br.com.vrosa.witchcraft.paper.item.ItemFactory;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.platform.Hotbar;
import br.com.vrosa.witchcraft.platform.ToolType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
        if (ItemFactory.toolOf(event.getItem()) != ToolType.PENCIL) return;

        final var action = event.getAction();
        final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        service.handlePencil(BukkitPlayer.of(event.getPlayer()), right);
    }

    @EventHandler
    public void onScroll(@NotNull PlayerItemHeldEvent event) {
        final var player = event.getPlayer();
        if (!player.isSneaking()) return;

        final var held = player.getInventory().getItem(event.getPreviousSlot());
        if (ItemFactory.toolOf(held) != ToolType.PENCIL) return;

        event.setCancelled(true);
        service.changeRadius(BukkitPlayer.of(player), Hotbar.scrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }
}
