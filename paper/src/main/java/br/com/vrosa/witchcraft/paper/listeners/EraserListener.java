package br.com.vrosa.witchcraft.paper.listeners;

import br.com.vrosa.witchcraft.core.erase.EraserService;
import br.com.vrosa.witchcraft.paper.item.ItemFactory;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.platform.Hotbar;
import br.com.vrosa.witchcraft.platform.ToolType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.event.block.Action.LEFT_CLICK_AIR;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public final class EraserListener implements Listener {

    private final EraserService service;

    public EraserListener(@NotNull EraserService service) {
        this.service = service;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (ItemFactory.toolOf(event.getItem()) != ToolType.ERASER) return;

        final var action = event.getAction();
        final boolean right = action == RIGHT_CLICK_AIR || action == RIGHT_CLICK_BLOCK;
        final boolean left = action == LEFT_CLICK_AIR || action == LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        if (!right) return;

        final var player = BukkitPlayer.of(event.getPlayer());
        if (event.getPlayer().isSneaking()) service.cycleMode(player);
        else service.toggleErasing(player);
    }

    @EventHandler
    public void onScroll(@NotNull PlayerItemHeldEvent event) {
        final var player = event.getPlayer();
        if (!player.isSneaking()) return;

        final var held = player.getInventory().getItem(event.getPreviousSlot());
        if (ItemFactory.toolOf(held) != ToolType.ERASER) return;

        event.setCancelled(true);
        service.changeRadius(BukkitPlayer.of(player), Hotbar.scrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }
}
