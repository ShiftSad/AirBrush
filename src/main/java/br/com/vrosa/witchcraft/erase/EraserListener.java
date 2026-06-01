package br.com.vrosa.witchcraft.erase;

import br.com.vrosa.witchcraft.history.History;
import br.com.vrosa.witchcraft.keys.ItemDefinition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import static org.bukkit.event.block.Action.*;

public final class EraserListener implements Listener {

    private final EraserService service;
    private final History history;

    public EraserListener(@NotNull EraserService service, @NotNull History history) {
        this.service = service;
        this.history = history;
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        final var item = event.getItem();
        if (item == null || ItemDefinition.getType(item) != ItemDefinition.ERASER) return;

        final var action = event.getAction();
        final boolean right = action == RIGHT_CLICK_AIR || action == RIGHT_CLICK_BLOCK;
        final boolean left = action == LEFT_CLICK_AIR || action == LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        event.setCancelled(true);
        if (!right) return;

        final var player = event.getPlayer();
        if (player.isSneaking()) service.cycleMode(player);
        else service.toggleErasing(player);
    }

    @EventHandler
    public void onScroll(@NotNull PlayerItemHeldEvent event) {
        final var player = event.getPlayer();
        if (!player.isSneaking()) return;

        final var held = player.getInventory().getItem(event.getPreviousSlot());
        if (held == null || ItemDefinition.getType(held) != ItemDefinition.ERASER) return;

        event.setCancelled(true);
        service.changeRadius(player, scrollDirection(event.getPreviousSlot(), event.getNewSlot()));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        service.remove(event.getPlayer());
        history.clear(event.getPlayer());
    }

    private static int scrollDirection(int previous, int next) {
        int raw = next - previous;
        if (raw > 4) raw -= 9;
        if (raw < -4) raw += 9;
        return Integer.signum(raw);
    }
}
