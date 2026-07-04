package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GlyphStrokeStore {

    private final Map<WorldRef, List<GlyphStroke>> strokes = new HashMap<>();

    public void add(@NotNull GlyphStroke stroke) {
        strokes.computeIfAbsent(stroke.world(), world -> new ArrayList<>()).add(stroke);
    }

    public @NotNull List<GlyphStroke> near(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        final var inWorld = strokes.get(world);
        if (inWorld == null) return List.of();

        final long now = System.currentTimeMillis();
        inWorld.removeIf(stroke -> !stroke.alive(now));

        final double r2 = radius * radius;
        final var result = new ArrayList<GlyphStroke>();
        for (final var stroke : inWorld) {
            if (stroke.centroid().distanceSquared(center) <= r2) result.add(stroke);
        }
        return result;
    }

    public void remove(@NotNull GlyphStroke stroke) {
        final var inWorld = strokes.get(stroke.world());
        if (inWorld != null) inWorld.remove(stroke);
    }

    public void prune(long now) {
        strokes.values().forEach(inWorld -> inWorld.removeIf(stroke -> !stroke.alive(now)));
        strokes.values().removeIf(List::isEmpty);
    }

    public void clear() {
        strokes.clear();
    }
}
