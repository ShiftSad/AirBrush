package br.com.vrosa.witchcraft;

import br.com.vrosa.witchcraft.color.ColorService;
import br.com.vrosa.witchcraft.color.PalletListener;
import br.com.vrosa.witchcraft.commands.ColorCommand;
import br.com.vrosa.witchcraft.commands.ItemCommand;
import br.com.vrosa.witchcraft.commands.UndoCommand;
import br.com.vrosa.witchcraft.draw.DrawListener;
import br.com.vrosa.witchcraft.draw.DrawService;
import br.com.vrosa.witchcraft.erase.EraserListener;
import br.com.vrosa.witchcraft.erase.EraserService;
import br.com.vrosa.witchcraft.history.History;
import br.com.vrosa.witchcraft.raycast.Raycaster;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitchCraft extends JavaPlugin {

    private final Raycaster raycaster = new Raycaster();
    private final History history = new History();
    private final DrawService drawService = new DrawService(raycaster, history);
    private final EraserService eraserService = new EraserService(raycaster, history);
    private final ColorService colorService = new ColorService(drawService);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DrawListener(drawService), this);
        getServer().getPluginManager().registerEvents(new EraserListener(eraserService, history), this);
        getServer().getPluginManager().registerEvents(new PalletListener(colorService), this);
        getServer().getScheduler().runTaskTimer(this, this::tick, 0L, 1L);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(ColorCommand.build(drawService), "Troca a cor do lápis.");
            event.registrar().register(ItemCommand.build(), "Pega os itens maneiros");
            event.registrar().register(UndoCommand.build(history), "Desfaz as últimas alterações.");
        });
    }

    private void tick() {
        for (final var player : Bukkit.getOnlinePlayers()) {
            raycaster.tick(player, !drawService.isActive(player) && eraserService.isNotHolding(player));
            drawService.tick(player);
            eraserService.tick(player);
            colorService.tick(player);
        }
    }
}
