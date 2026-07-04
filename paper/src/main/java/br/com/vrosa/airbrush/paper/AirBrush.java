package br.com.vrosa.airbrush.paper;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.resourcepack.ResourcePackService;
import br.com.vrosa.airbrush.paper.commands.ColorCommand;
import br.com.vrosa.airbrush.paper.commands.GlyphTestCommand;
import br.com.vrosa.airbrush.paper.commands.ItemCommand;
import br.com.vrosa.airbrush.paper.commands.UndoCommand;
import br.com.vrosa.airbrush.paper.commands.AirBrushCommand;
import br.com.vrosa.airbrush.paper.config.PaperConfig;
import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.listeners.CauldronListener;
import br.com.vrosa.airbrush.paper.listeners.DeathListener;
import br.com.vrosa.airbrush.paper.listeners.DrawListener;
import br.com.vrosa.airbrush.paper.listeners.EraserListener;
import br.com.vrosa.airbrush.paper.listeners.HammerListener;
import br.com.vrosa.airbrush.paper.listeners.JoinListener;
import br.com.vrosa.airbrush.paper.listeners.MarkerListener;
import br.com.vrosa.airbrush.paper.listeners.PaletteListener;
import br.com.vrosa.airbrush.paper.listeners.QuitListener;
import br.com.vrosa.airbrush.paper.listeners.SegmentBreakListener;
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
    private CauldronListener cauldrons;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PaperConfig.load(config, getConfig());
        Messages.load(getDataFolder().toPath().resolve("lang"));

        final var raycaster = new BukkitRaycaster(config::maxRaycastLength);
        engine = new AirBrushEngine(new BukkitPlatform(), raycaster, config);
        resourcePack.start();

        getServer().addRecipe(ItemFactory.hammerRecipe());

        final var pm = getServer().getPluginManager();
        pm.registerEvents(new DrawListener(engine.drawService()), this);
        pm.registerEvents(new MarkerListener(engine.markerService()), this);
        pm.registerEvents(new EraserListener(engine.eraserService()), this);
        pm.registerEvents(new HammerListener(engine), this);
        pm.registerEvents(new PaletteListener(engine.colorService()), this);
        cauldrons = new CauldronListener(engine.cauldronService());
        pm.registerEvents(cauldrons, this);
        for (final var world : getServer().getWorlds()) {
            for (final var chunk : world.getLoadedChunks()) cauldrons.restore(chunk);
        }
        pm.registerEvents(new SegmentBreakListener(engine), this);
        pm.registerEvents(new QuitListener(engine), this);
        pm.registerEvents(new DeathListener(engine), this);
        pm.registerEvents(new JoinListener(resourcePack), this);

        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(ColorCommand.build(engine.drawService()), "Changes the pencil color.");
            event.registrar().register(ItemCommand.build(), "Gives tools (admin).");
            event.registrar().register(UndoCommand.build(engine.history()), "Undoes the last changes.");
            event.registrar().register(AirBrushCommand.build(this::reload), "Administers AirBrush.");
            event.registrar().register(GlyphTestCommand.build(engine, this), "Analyzes nearby marker glyphs.");
        });
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private void reload() {
        shutdownSessions();
        reloadConfig();
        PaperConfig.load(config, getConfig());
        Messages.load(getDataFolder().toPath().resolve("lang"));
        for (final var world : getServer().getWorlds()) {
            for (final var chunk : world.getLoadedChunks()) cauldrons.restore(chunk);
        }
    }

    private void shutdown() {
        shutdownSessions();
        resourcePack.stop();
    }

    private void shutdownSessions() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            engine.handleQuit(BukkitPlayer.of(player));
        }
        engine.shutdownGlobals();
        cauldrons.clearTracked();
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            engine.tick(BukkitPlayer.of(player));
        }
        engine.tickSegments();
        cauldrons.tick();
    }
}
