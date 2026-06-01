package br.com.vrosa.witchcraft.draw;

import br.com.vrosa.witchcraft.keys.ItemDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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
    public void onScroll(@NotNull PlayerItemHeldEvent event) {
        final var player = event.getPlayer();
        if (!player.isSneaking()) return;

        final var held = player.getInventory().getItem(event.getPreviousSlot());
        if (held == null || ItemDefinition.getType(held) != ItemDefinition.PENCIL) return;

        event.setCancelled(true);
        service.changeRadius(player, scrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        service.remove(event.getPlayer());
    }

    private static int scrollDirection(int previous, int next) {
        int raw = next - previous;
        if (raw > 4) raw -= 9;
        if (raw < -4) raw += 9;
        return Integer.signum(raw);
    }
}
