package br.com.vrosa.airbrush.core.glyph.craft;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.glyph.GlyphStrokeStore;
import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.geometry.StrokeGeometry;
import br.com.vrosa.airbrush.core.glyph.model.Glyph;
import br.com.vrosa.airbrush.platform.CarriedInk;
import br.com.vrosa.airbrush.platform.DropHandle;
import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.Pose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlyphCraftService {

    private static final double MAX_PLANE_DISTANCE = 1.0;

    private final Platform platform;
    private final GlyphStrokeStore strokes;
    private final AirBrushConfig config;

    public GlyphCraftService(@NotNull Platform platform, @NotNull GlyphStrokeStore strokes,
                             @NotNull AirBrushConfig config) {
        this.platform = platform;
        this.strokes = strokes;
        this.config = config;
    }

    public boolean attempt(@NotNull Pose pointer) {
        final var nearby = strokes.near(pointer.world(), pointer.position(), config.glyphAnalysisRadius());
        final var analysis = GlyphAnalyzer.analyze(nearby,
                new GlyphAnalyzer.Settings(config.glyphMinRadius(), config.glyphMaxRadius()));
        final var glyph = analysis.glyph().orElse(null);
        if (glyph == null) return false;

        final var drops = ingredientsInside(glyph);
        final var available = new HashMap<String, Integer>();
        for (final var drop : drops) available.merge(drop.itemId(), drop.amount(), Integer::sum);

        final var recipe = GlyphRecipes.match(glyph.base(), glyph.modifiers(), available).orElse(null);
        if (recipe == null) return false;

        final var carried = carriedInk(drops);
        consume(drops, recipe.ingredients());
        for (final var stroke : glyph.strokes()) {
            for (final var segment : stroke.segments()) {
                if (segment.isValid()) segment.remove();
            }
        }

        platform.dropCraftResult(glyph.world(), glyph.center(), recipe.id(), glyph.quality(), carried);
        platform.craftEffects(glyph.world(), glyph.center());
        return true;
    }

    /** Ink from a quill consumed in the craft is carried over to the resulting quill. */
    private static @Nullable CarriedInk carriedInk(@NotNull List<DropHandle> drops) {
        for (final var drop : drops) {
            final var ink = drop.inkId();
            if (ink == null) continue;
            final var color = drop.inkColor();
            return new CarriedInk(ink, color == null ? 0 : color, drop.durabilityRemaining());
        }
        return null;
    }

    private @NotNull List<DropHandle> ingredientsInside(@NotNull Glyph glyph) {
        final var candidates = platform.droppedItemsWithin(glyph.world(), glyph.center(),
                config.glyphAnalysisRadius());
        final var inside = new ArrayList<DropHandle>();
        for (final var drop : candidates) {
            if (Math.abs(glyph.plane().distance(drop.position())) > MAX_PLANE_DISTANCE) continue;
            if (!StrokeGeometry.pointInPolygon(glyph.plane().project(drop.position()), glyph.basePolygon())) continue;
            inside.add(drop);
        }
        return inside;
    }

    private static void consume(@NotNull List<DropHandle> drops, @NotNull Map<String, Integer> ingredients) {
        for (final var entry : ingredients.entrySet()) {
            int remaining = entry.getValue();
            for (final var drop : drops) {
                if (remaining <= 0) break;
                if (!drop.itemId().equals(entry.getKey())) continue;
                final int take = Math.min(remaining, drop.amount());
                drop.consume(take);
                remaining -= take;
            }
        }
    }
}
