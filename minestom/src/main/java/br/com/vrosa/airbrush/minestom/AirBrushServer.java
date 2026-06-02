package br.com.vrosa.airbrush.minestom;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.resourcepack.ResourcePackService;
import br.com.vrosa.airbrush.minestom.commands.ColorCommand;
import br.com.vrosa.airbrush.minestom.commands.ItemCommand;
import br.com.vrosa.airbrush.minestom.commands.UndoCommand;
import br.com.vrosa.airbrush.minestom.commands.AirBrushCommand;
import br.com.vrosa.airbrush.minestom.config.MinestomConfig;
import br.com.vrosa.airbrush.minestom.item.MinestomItems;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlatform;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import br.com.vrosa.airbrush.minestom.platform.MinestomRaycaster;
import br.com.vrosa.airbrush.platform.Hotbar;
import br.com.vrosa.airbrush.platform.ToolType;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;

import java.nio.file.Path;

public final class AirBrushServer {

    private static final Pos SPAWN = new Pos(0.5, 1.0, 0.5);

    static void main() {
        final var server = MinecraftServer.init(new Auth.Online());

        final var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.GLASS));

        final var config = new AirBrushConfig();
        MinestomConfig.load(config);
        Messages.load(Path.of("lang"));

        final var engine = new AirBrushEngine(
                new MinestomPlatform(), new MinestomRaycaster(config::maxRaycastLength), config);

        final var resourcePack = new ResourcePackService(config);
        resourcePack.start();
        MinecraftServer.getSchedulerManager().buildShutdownTask(resourcePack::stop);

        registerEvents(instance, engine, resourcePack);
        registerCommands(engine, () -> {
            MinestomConfig.load(config);
            Messages.load(Path.of("lang"));
        });
        scheduleTick(instance, engine);

        server.start("0.0.0.0", 25565);
        System.out.println("AirBrush Minestom server iniciado em 0.0.0.0:25565");
    }

    private static void registerEvents(Instance instance, AirBrushEngine engine, ResourcePackService resourcePack) {
        final var events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(SPAWN);
        });

        events.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) return;
            final var player = event.getPlayer();
            player.setGameMode(GameMode.CREATIVE);
            final var wp = MinestomPlayer.of(player);
            resourcePack.apply(wp);
            wp.giveTool(ToolType.PENCIL);
            wp.giveTool(ToolType.ERASER);
            wp.giveTool(ToolType.PALETTE);
        });

        events.addListener(PlayerDisconnectEvent.class,
                event -> engine.handleQuit(MinestomPlayer.of(event.getPlayer())));

        events.addListener(PlayerUseItemEvent.class, event -> dispatchRight(engine, event.getPlayer()));
        events.addListener(PlayerBlockInteractEvent.class, event -> dispatchRight(engine, event.getPlayer()));

        events.addListener(PlayerHandAnimationEvent.class, event -> {
            if (event.getHand() == PlayerHand.MAIN) dispatchLeft(engine, event.getPlayer());
        });

        events.addListener(PlayerBlockBreakEvent.class, event -> {
            if (MinestomPlayer.of(event.getPlayer()).holdingAnyTool()) event.setCancelled(true);
        });

        events.addListener(PlayerChangeHeldSlotEvent.class, event -> {
            final var player = event.getPlayer();
            if (!player.isSneaking()) return;

            final var tool = MinestomItems.toolOf(event.getItemInOldSlot());
            if (tool == null) return;

            final int direction = Hotbar.scrollDirection(event.getOldSlot(), event.getNewSlot());
            final var wp = MinestomPlayer.of(player);
            if (tool == ToolType.PENCIL) engine.drawService().changeRadius(wp, direction);
            else if (tool == ToolType.ERASER) engine.eraserService().changeRadius(wp, direction);
            event.setCancelled(true);
        });
    }

    private static void dispatchRight(AirBrushEngine engine, Player player) {
        final var wp = MinestomPlayer.of(player);
        final var tool = wp.heldTool();
        if (tool == null) return;
        switch (tool) {
            case PENCIL -> engine.drawService().handlePencil(wp, true);
            case ERASER -> {
                if (player.isSneaking()) engine.eraserService().cycleMode(wp);
                else engine.eraserService().toggleErasing(wp);
            }
            case PALETTE -> engine.colorService().rightClick(wp);
        }
    }

    private static void dispatchLeft(AirBrushEngine engine, Player player) {
        final var wp = MinestomPlayer.of(player);
        final var tool = wp.heldTool();
        if (tool == null) return;
        switch (tool) {
            case PENCIL -> engine.drawService().handlePencil(wp, false);
            case PALETTE -> {
                if (engine.colorService().isOpen(wp)) engine.colorService().close(wp);
            }
            case ERASER -> { }
        }
    }

    private static void registerCommands(AirBrushEngine engine, Runnable reload) {
        final var commands = MinecraftServer.getCommandManager();
        commands.register(new ColorCommand(engine.drawService()));
        commands.register(new ItemCommand());
        commands.register(new UndoCommand(engine.history()));
        commands.register(new AirBrushCommand(reload));
    }

    private static void scheduleTick(Instance instance, AirBrushEngine engine) {
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            for (final var player : instance.getPlayers()) {
                engine.tick(MinestomPlayer.of(player));
            }
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    private AirBrushServer() {}
}
