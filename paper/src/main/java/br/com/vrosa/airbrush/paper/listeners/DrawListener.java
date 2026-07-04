package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.draw.DrawService;
import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import br.com.vrosa.airbrush.platform.Hotbar;
import br.com.vrosa.airbrush.platform.ToolType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DrawListener implements Listener {

    private final DrawService service;

    public DrawListener(@NotNull DrawService service) {
        this.service = service;
    }

    // No ignoreCancelled: air clicks arrive with useInteractedBlock() == DENY,
    // which Bukkit reports as a cancelled event.
    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!isDrawTool(ItemFactory.toolOf(event.getItem()))) return;

        final var action = event.getAction();
        final boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        final boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        service.handlePencil(BukkitPlayer.of(event.getPlayer()), right);
    }

    @EventHandler
    public void onSlotChange(@NotNull PlayerItemHeldEvent event) {
        if (event.getPlayer().isSneaking()) return;
        service.confirmActive(BukkitPlayer.of(event.getPlayer()));
    }

    @EventHandler
    public void onScroll(@NotNull PlayerItemHeldEvent event) {
        final var player = event.getPlayer();
        if (!player.isSneaking()) return;

        final var held = player.getInventory().getItem(event.getPreviousSlot());
        if (!isDrawTool(ItemFactory.toolOf(held))) return;

        event.setCancelled(true);
        service.changeRadius(BukkitPlayer.of(player), Hotbar.scrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }

    private static boolean isDrawTool(@Nullable ToolType tool) {
        return tool == ToolType.PENCIL || tool == ToolType.QUILL;
    }
}
