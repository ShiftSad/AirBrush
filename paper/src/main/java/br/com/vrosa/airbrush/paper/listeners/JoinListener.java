package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.resourcepack.ResourcePackService;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public final class JoinListener implements Listener {

    private final ResourcePackService resourcePack;

    public JoinListener(@NotNull ResourcePackService resourcePack) {
        this.resourcePack = resourcePack;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        resourcePack.apply(BukkitPlayer.of(event.getPlayer()));
    }
}
