package br.com.vrosa.witchcraft;

import br.com.vrosa.witchcraft.draw.DrawListener;
import br.com.vrosa.witchcraft.draw.DrawService;
import br.com.vrosa.witchcraft.raycast.Raycaster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitchCraft extends JavaPlugin {

    private final Raycaster raycaster = new Raycaster();
    private final DrawService drawService = new DrawService(raycaster);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DrawListener(drawService), this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            raycaster.tick(player, !drawService.isActive(player));
            drawService.tick(player);
        }
    }
}
