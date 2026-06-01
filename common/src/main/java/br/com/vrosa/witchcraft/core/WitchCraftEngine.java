package br.com.vrosa.witchcraft.core;

import br.com.vrosa.witchcraft.core.color.ColorService;
import br.com.vrosa.witchcraft.core.draw.DrawService;
import br.com.vrosa.witchcraft.core.erase.EraserService;
import br.com.vrosa.witchcraft.core.history.History;
import br.com.vrosa.witchcraft.platform.Platform;
import br.com.vrosa.witchcraft.platform.Raycaster;
import br.com.vrosa.witchcraft.platform.WPlayer;
import org.jetbrains.annotations.NotNull;

public final class WitchCraftEngine {

    private final Raycaster raycaster;
    private final History history;
    private final DrawService drawService;
    private final EraserService eraserService;
    private final ColorService colorService;

    public WitchCraftEngine(@NotNull Platform platform, @NotNull Raycaster raycaster) {
        this.raycaster = raycaster;
        this.history = new History(platform);
        this.drawService = new DrawService(platform, raycaster, history);
        this.eraserService = new EraserService(platform, raycaster, history);
        this.colorService = new ColorService(platform, drawService);
    }

    public @NotNull Raycaster raycaster() {
        return raycaster;
    }

    public @NotNull History history() {
        return history;
    }

    public @NotNull DrawService drawService() {
        return drawService;
    }

    public @NotNull EraserService eraserService() {
        return eraserService;
    }

    public @NotNull ColorService colorService() {
        return colorService;
    }

    public void tick(@NotNull WPlayer player) {
        raycaster.tick(player, !drawService.isActive(player) && eraserService.isNotHolding(player));
        drawService.tick(player);
        eraserService.tick(player);
        colorService.tick(player);
    }

    public void handleQuit(@NotNull WPlayer player) {
        drawService.remove(player);
        eraserService.remove(player);
        colorService.close(player);
        history.clear(player);
    }
}
