package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public final class DeathListener implements Listener {

    private final AirBrushEngine engine;

    public DeathListener(@NotNull AirBrushEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(@NotNull PlayerDeathEvent event) {
        engine.handleQuit(BukkitPlayer.of(event.getEntity()));
    }
}
