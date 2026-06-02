package br.com.vrosa.witchcraft.paper;

import br.com.vrosa.witchcraft.core.WitchCraftEngine;
import br.com.vrosa.witchcraft.core.config.WitchCraftConfig;
import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.core.resourcepack.ResourcePackService;
import br.com.vrosa.witchcraft.paper.commands.ColorCommand;
import br.com.vrosa.witchcraft.paper.commands.ItemCommand;
import br.com.vrosa.witchcraft.paper.commands.UndoCommand;
import br.com.vrosa.witchcraft.paper.commands.WitchCraftCommand;
import br.com.vrosa.witchcraft.paper.config.PaperConfig;
import br.com.vrosa.witchcraft.paper.listeners.DrawListener;
import br.com.vrosa.witchcraft.paper.listeners.EraserListener;
import br.com.vrosa.witchcraft.paper.listeners.JoinListener;
import br.com.vrosa.witchcraft.paper.listeners.PaletteListener;
import br.com.vrosa.witchcraft.paper.listeners.QuitListener;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlatform;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.paper.platform.BukkitRaycaster;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitchCraft extends JavaPlugin {

    private final WitchCraftConfig config = new WitchCraftConfig();
    private final ResourcePackService resourcePack = new ResourcePackService(config);
    private WitchCraftEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PaperConfig.load(config, getConfig());
        Messages.load(getDataFolder().toPath().resolve("lang"));

        final var raycaster = new BukkitRaycaster(config::maxRaycastLength);
        engine = new WitchCraftEngine(new BukkitPlatform(), raycaster, config);
        resourcePack.start();

        final var pm = getServer().getPluginManager();
        pm.registerEvents(new DrawListener(engine.drawService()), this);
        pm.registerEvents(new EraserListener(engine.eraserService()), this);
        pm.registerEvents(new PaletteListener(engine.colorService()), this);
        pm.registerEvents(new QuitListener(engine), this);
        pm.registerEvents(new JoinListener(resourcePack), this);

        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(ColorCommand.build(engine.drawService()), "Troca a cor do lápis.");
            event.registrar().register(ItemCommand.build(), "Pega os itens maneiros");
            event.registrar().register(UndoCommand.build(engine.history()), "Desfaz as últimas alterações.");
            event.registrar().register(WitchCraftCommand.build(this::reload), "Administra o WitchCraft.");
        });
    }

    @Override
    public void onDisable() {
        resourcePack.stop();
    }

    private void reload() {
        reloadConfig();
        PaperConfig.load(config, getConfig());
        Messages.load(getDataFolder().toPath().resolve("lang"));
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            engine.tick(BukkitPlayer.of(player));
        }
    }
}
