package br.com.vrosa.witchcraft.paper.listeners;

import br.com.vrosa.witchcraft.core.WitchCraftEngine;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class QuitListener implements Listener {

    private final WitchCraftEngine engine;

    public QuitListener(@NotNull WitchCraftEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        engine.handleQuit(BukkitPlayer.of(event.getPlayer()));
    }
}
