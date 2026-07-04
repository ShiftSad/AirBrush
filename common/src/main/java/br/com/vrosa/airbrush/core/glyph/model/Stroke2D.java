package br.com.vrosa.airbrush.core.glyph.model;

import br.com.vrosa.airbrush.core.glyph.GlyphStroke;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.List;

public record Stroke2D(@NotNull GlyphStroke source, @NotNull List<Vector2d> points) {

    public Stroke2D {
        points = List.copyOf(points);
    }
}
