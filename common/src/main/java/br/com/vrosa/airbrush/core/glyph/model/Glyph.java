package br.com.vrosa.airbrush.core.glyph.model;

import br.com.vrosa.airbrush.core.glyph.GlyphStroke;
import br.com.vrosa.airbrush.core.glyph.geometry.StrokeGeometry;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.List;
import java.util.Set;

public record Glyph(@NotNull BaseShape base, @NotNull Set<Modifier> modifiers, double quality,
                    @NotNull List<Vector2d> basePolygon, @NotNull Vec3 center, @NotNull WorldRef world,
                    @NotNull StrokeGeometry.Plane plane, @NotNull List<GlyphStroke> strokes) {

    public Glyph {
        modifiers = Set.copyOf(modifiers);
        basePolygon = List.copyOf(basePolygon);
        strokes = List.copyOf(strokes);
    }
}
