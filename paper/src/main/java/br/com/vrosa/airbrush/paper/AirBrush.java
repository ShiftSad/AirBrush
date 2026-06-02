package br.com.vrosa.airbrush.paper;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.resourcepack.ResourcePackService;
import br.com.vrosa.airbrush.paper.commands.ColorCommand;
import br.com.vrosa.airbrush.paper.commands.ItemCommand;
import br.com.vrosa.airbrush.paper.commands.UndoCommand;
import br.com.vrosa.airbrush.paper.commands.AirBrushCommand;
import br.com.vrosa.airbrush.paper.config.PaperConfig;
import br.com.vrosa.airbrush.paper.listeners.DrawListener;
import br.com.vrosa.airbrush.paper.listeners.EraserListener;
import br.com.vrosa.airbrush.paper.listeners.JoinListener;
import br.com.vrosa.airbrush.paper.listeners.PaletteListener;
import br.com.vrosa.airbrush.paper.listeners.QuitListener;
import br.com.vrosa.airbrush.paper.platform.BukkitPlatform;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import br.com.vrosa.airbrush.paper.platform.BukkitRaycaster;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AirBrush extends JavaPlugin {

    private final AirBrushConfig config = new AirBrushConfig();
    private final ResourcePackService resourcePack = new ResourcePackService(config);
    private AirBrushEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PaperConfig.load(config, getConfig());
        Messages.load(getDataFolder().toPath().resolve("lang"));

        final var raycaster = new BukkitRaycaster(config::maxRaycastLength);
        engine = new AirBrushEngine(new BukkitPlatform(), raycaster, config);
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
            event.registrar().register(AirBrushCommand.build(this::reload), "Administra o AirBrush.");
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
