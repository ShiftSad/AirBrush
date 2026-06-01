package br.com.vrosa.witchcraft.paper.listeners;

import br.com.vrosa.witchcraft.core.color.ColorService;
import br.com.vrosa.witchcraft.paper.item.ItemFactory;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.platform.ToolType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public final class PalletListener implements Listener {

    private final ColorService service;

    public PalletListener(@NotNull ColorService service) {
        this.service = service;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (ItemFactory.toolOf(event.getItem()) != ToolType.PALLET) return;

        final var action = event.getAction();
        final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        final var player = BukkitPlayer.of(event.getPlayer());

        if (right) service.rightClick(player);
        else if (service.isOpen(player)) service.close(player);
    }
}
