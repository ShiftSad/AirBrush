package br.com.vrosa.airbrush.core;

import br.com.vrosa.airbrush.core.color.ColorService;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.draw.DrawService;
import br.com.vrosa.airbrush.core.draw.MarkerService;
import br.com.vrosa.airbrush.core.erase.EraserService;
import br.com.vrosa.airbrush.core.glyph.GlyphStrokeStore;
import br.com.vrosa.airbrush.core.glyph.craft.GlyphCraftService;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.ink.CauldronService;
import br.com.vrosa.airbrush.core.ui.ActionBars;
import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.Raycaster;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;

public final class AirBrushEngine {

    private final Platform platform;
    private final AirBrushConfig config;
    private final Raycaster raycaster;
    private final History history;
    private final ActionBars bars;
    private final GlyphStrokeStore glyphStrokes = new GlyphStrokeStore();
    private final GlyphCraftService glyphCraft;
    private final DrawService drawService;
    private final MarkerService markerService;
    private final EraserService eraserService;
    private final ColorService colorService;
    private final CauldronService cauldronService;

    public AirBrushEngine(@NotNull Platform platform, @NotNull Raycaster raycaster, @NotNull AirBrushConfig config) {
        this.platform = platform;
        this.config = config;
        this.raycaster = raycaster;
        this.history = new History(platform);
        this.bars = new ActionBars();
        this.glyphCraft = new GlyphCraftService(platform, glyphStrokes, config);
        this.drawService = new DrawService(platform, raycaster, history, config, bars);
        this.markerService = new MarkerService(platform, raycaster, history, config, glyphStrokes, bars);
        this.eraserService = new EraserService(platform, raycaster, history, config, bars);
        this.colorService = new ColorService(platform, drawService);
        this.cauldronService = new CauldronService(platform, config, bars);
    }

    public @NotNull GlyphStrokeStore glyphStrokes() {
        return glyphStrokes;
    }

    public @NotNull GlyphCraftService glyphCraft() {
        return glyphCraft;
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

    public @NotNull CauldronService cauldronService() {
        return cauldronService;
    }

    public void tick(@NotNull WPlayer player) {
        raycaster.tick(player, !drawService.isActive(player) && !markerService.isActive(player)
                && eraserService.isNotHolding(player));
        drawService.tick(player);
        markerService.tick(player);
        eraserService.tick(player);
        colorService.tick(player);
        cauldronService.gazeTick(player, raycaster.current(player));
    }

    public void tickSegments() {
        markerService.tickSegments();
        drawService.tickSegments();
        cauldronService.tick();
    }

    public void handleQuit(@NotNull WPlayer player) {
        drawService.remove(player);
        markerService.remove(player);
        eraserService.remove(player);
        colorService.remove(player);
        bars.clear(player);
        cauldronService.clearPlayer(player);
        history.clear(player);
        raycaster.clear(player);
    }

    /** Removes segments drawn on {@code block} (block coords) — nothing floats over a broken block. */
    public void breakAnchored(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var center = new Vec3(block.x() + 0.5, block.y() + 0.5, block.z() + 0.5);
        for (final var segment : platform.segmentsWithin(world, center, 2.0)) {
            if (segment.isValid() && block.equals(segment.anchorBlock())) segment.remove();
        }
    }

    /** Discards in-memory global state (displays, active brews, glyphs). */
    public void shutdownGlobals() {
        drawService.clearGlobals();
        markerService.clearGlobals();
        cauldronService.shutdown();
        glyphStrokes.clear();
    }
}
