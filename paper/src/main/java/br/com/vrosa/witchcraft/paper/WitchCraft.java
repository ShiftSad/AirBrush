package br.com.vrosa.witchcraft.paper;

import br.com.vrosa.witchcraft.core.WitchCraftEngine;
import br.com.vrosa.witchcraft.paper.commands.ColorCommand;
import br.com.vrosa.witchcraft.paper.commands.ItemCommand;
import br.com.vrosa.witchcraft.paper.commands.UndoCommand;
import br.com.vrosa.witchcraft.paper.listeners.DrawListener;
import br.com.vrosa.witchcraft.paper.listeners.EraserListener;
import br.com.vrosa.witchcraft.paper.listeners.PalletListener;
import br.com.vrosa.witchcraft.paper.listeners.QuitListener;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlatform;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.paper.platform.BukkitRaycaster;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitchCraft extends JavaPlugin {

    private final WitchCraftEngine engine = new WitchCraftEngine(new BukkitPlatform(), new BukkitRaycaster());

    @Override
    public void onEnable() {
        final var pm = getServer().getPluginManager();
        pm.registerEvents(new DrawListener(engine.drawService()), this);
        pm.registerEvents(new EraserListener(engine.eraserService()), this);
        pm.registerEvents(new PalletListener(engine.colorService()), this);
        pm.registerEvents(new QuitListener(engine), this);

        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(ColorCommand.build(engine.drawService()), "Troca a cor do lápis.");
            event.registrar().register(ItemCommand.build(), "Pega os itens maneiros");
            event.registrar().register(UndoCommand.build(engine.history()), "Desfaz as últimas alterações.");
        });
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            engine.tick(BukkitPlayer.of(player));
        }
    }
}
