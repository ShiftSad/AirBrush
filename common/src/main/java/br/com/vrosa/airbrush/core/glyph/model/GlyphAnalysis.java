package br.com.vrosa.airbrush.core.glyph.model;

import br.com.vrosa.airbrush.core.glyph.geometry.QualityScorer;
import br.com.vrosa.airbrush.platform.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record GlyphAnalysis(@NotNull Optional<Glyph> glyph, @NotNull Reason reason,
                            @Nullable QualityScorer.Breakdown breakdown, @NotNull List<Vec3> corners) {

    public GlyphAnalysis {
        corners = List.copyOf(corners);
    }

    public static @NotNull GlyphAnalysis failure(@NotNull Reason reason) {
        return new GlyphAnalysis(Optional.empty(), reason, null, List.of());
    }
}
