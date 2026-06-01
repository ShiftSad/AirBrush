package br.com.vrosa.witchcraft;

import br.com.vrosa.witchcraft.color.ColorSession;
import br.com.vrosa.witchcraft.commands.ColorCommand;
import br.com.vrosa.witchcraft.commands.ItemCommand;
import br.com.vrosa.witchcraft.draw.DrawListener;
import br.com.vrosa.witchcraft.draw.DrawService;
import br.com.vrosa.witchcraft.raycast.Raycaster;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class WitchCraft extends JavaPlugin implements Listener {

    private final Raycaster raycaster = new Raycaster();
    private final DrawService drawService = new DrawService(raycaster);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new DrawListener(drawService), this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(ColorCommand.build(drawService), "Troca a cor do lápis.");
            event.registrar().register(ItemCommand.build(), "Pega os itens maneiros");
        });
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            raycaster.tick(player, !drawService.isActive(player));
            drawService.tick(player);
        }
    }

    @EventHandler
    public void onChat(@NotNull PlayerChatEvent event) {
        new ColorSession().build(event.getPlayer());
    }
}
