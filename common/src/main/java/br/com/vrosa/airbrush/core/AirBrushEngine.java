package br.com.vrosa.airbrush.core;

import br.com.vrosa.airbrush.core.color.ColorService;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.draw.DrawService;
import br.com.vrosa.airbrush.core.draw.MarkerService;
import br.com.vrosa.airbrush.core.erase.EraserService;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.Raycaster;
import br.com.vrosa.airbrush.platform.WPlayer;
import org.jetbrains.annotations.NotNull;

public final class AirBrushEngine {

    private final AirBrushConfig config;
    private final Raycaster raycaster;
    private final History history;
    private final DrawService drawService;
    private final MarkerService markerService;
    private final EraserService eraserService;
    private final ColorService colorService;

    public AirBrushEngine(@NotNull Platform platform, @NotNull Raycaster raycaster, @NotNull AirBrushConfig config) {
        this.config = config;
        this.raycaster = raycaster;
        this.history = new History(platform);
        this.drawService = new DrawService(platform, raycaster, history, config);
        this.markerService = new MarkerService(platform, raycaster, history, config);
        this.eraserService = new EraserService(platform, raycaster, history, config);
        this.colorService = new ColorService(platform, drawService);
    }

    public @NotNull AirBrushConfig config() {
        return config;
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

    public @NotNull MarkerService markerService() {
        return markerService;
    }

    public @NotNull EraserService eraserService() {
        return eraserService;
    }

    public @NotNull ColorService colorService() {
        return colorService;
    }

    public void tick(@NotNull WPlayer player) {
        raycaster.tick(player, !drawService.isActive(player) && !markerService.isActive(player)
                && eraserService.isNotHolding(player));
        drawService.tick(player);
        markerService.tick(player);
        eraserService.tick(player);
        colorService.tick(player);
    }

    public void tickSegments() {
        markerService.tickSegments();
    }

    public void handleQuit(@NotNull WPlayer player) {
        drawService.remove(player);
        markerService.remove(player);
        eraserService.remove(player);
        colorService.close(player);
        history.clear(player);
    }
}
