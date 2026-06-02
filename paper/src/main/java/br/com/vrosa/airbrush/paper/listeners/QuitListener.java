package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class QuitListener implements Listener {

    private final AirBrushEngine engine;

    public QuitListener(@NotNull AirBrushEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        engine.handleQuit(BukkitPlayer.of(event.getPlayer()));
    }
}
