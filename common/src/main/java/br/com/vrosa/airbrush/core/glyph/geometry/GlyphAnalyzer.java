package br.com.vrosa.airbrush.core.glyph.geometry;

import br.com.vrosa.airbrush.core.glyph.GlyphStroke;
import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.Glyph;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.core.glyph.model.Stroke2D;
import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GlyphAnalyzer {

    public static final double MAX_PLANARITY_RMS = 0.35;

    private GlyphAnalyzer() {}

    public record Settings(double minRadius, double maxRadius) {}

    public static @NotNull GlyphAnalysis analyze(@NotNull List<GlyphStroke> strokes, @NotNull Settings settings) {
        if (strokes.isEmpty()) return GlyphAnalysis.failure(Reason.NO_STROKES);

        final var world = strokes.getFirst().world();
        for (final var stroke : strokes) {
            if (!stroke.world().equals(world)) return GlyphAnalysis.failure(Reason.MIXED_WORLDS);
        }

        final var allPoints = new ArrayList<Vec3>();
        for (final var stroke : strokes) {
            for (final var sample : stroke.rawSamples()) allPoints.add(sample.position());
        }
        if (allPoints.size() < 3) return GlyphAnalysis.failure(Reason.NO_STROKES);

        final var plane = StrokeGeometry.fitPlane(allPoints);
        if (StrokeGeometry.planarityRms(allPoints, plane) > MAX_PLANARITY_RMS) {
            return GlyphAnalysis.failure(Reason.NOT_PLANAR);
        }

        final var projected = new ArrayList<Stroke2D>(strokes.size());
        for (final var stroke : strokes) {
            final var points = new ArrayList<Vector2d>(stroke.rawSamples().size());
            for (final var sample : stroke.rawSamples()) points.add(plane.project(sample.position()));
            projected.add(new Stroke2D(stroke, points));
        }

        Stroke2D baseStroke = null;
        ShapeClassifier.Fit baseFit = null;
        boolean anyClosed = false;
        boolean anyRecognized = false;
        boolean anyTooLarge = false;
        for (final var stroke : projected) {
            final var outcome = ShapeClassifier.classify(stroke.points());
            anyClosed |= outcome.closed();
            final var fit = outcome.fit();
            if (fit == null) continue;
            anyRecognized = true;
            if (fit.meanRadius() > settings.maxRadius()) {
                anyTooLarge = true;
                continue;
            }
            if (fit.meanRadius() < settings.minRadius()) continue;
            if (baseFit == null || fit.meanRadius() > baseFit.meanRadius()) {
                baseFit = fit;
                baseStroke = stroke;
            }
        }

        if (baseFit == null) {
            if (anyTooLarge) return GlyphAnalysis.failure(Reason.TOO_LARGE);
            if (anyRecognized) return GlyphAnalysis.failure(Reason.TOO_SMALL);
            if (anyClosed) return GlyphAnalysis.failure(Reason.UNRECOGNIZED_SHAPE);
            return GlyphAnalysis.failure(Reason.NOT_CLOSED);
        }

        final var others = new ArrayList<Stroke2D>(projected);
        others.remove(baseStroke);
        final var modifiers = ModifierClassifier.classify(baseFit, others);
        final var breakdown = QualityScorer.score(baseFit, modifiers.noiseCount());

        final var glyphStrokes = new ArrayList<GlyphStroke>();
        glyphStrokes.add(baseStroke.source());
        glyphStrokes.addAll(modifiers.matched());

        final var corners = new ArrayList<Vec3>(baseFit.corners().size());
        for (final var corner : baseFit.corners()) corners.add(plane.unproject(corner.position()));

        final var glyph = new Glyph(baseFit.shape(), modifiers.modifiers(), breakdown.total(),
                baseFit.ring(), plane.unproject(baseFit.centroid()), world, plane, glyphStrokes);
        return new GlyphAnalysis(Optional.of(glyph), Reason.OK, breakdown, corners);
    }

    public static @NotNull Optional<BaseShape> classifyStroke(@NotNull List<Pose> samples, @NotNull Settings settings) {
        if (samples.size() < 3) return Optional.empty();

        final var points = new ArrayList<Vec3>(samples.size());
        for (final var sample : samples) points.add(sample.position());

        final var plane = StrokeGeometry.fitPlane(points);
        if (StrokeGeometry.planarityRms(points, plane) > MAX_PLANARITY_RMS) return Optional.empty();

        final var projected = new ArrayList<Vector2d>(points.size());
        for (final var point : points) projected.add(plane.project(point));

        final var fit = ShapeClassifier.classify(projected).fit();
        if (fit == null) return Optional.empty();
        if (fit.meanRadius() < settings.minRadius() || fit.meanRadius() > settings.maxRadius()) {
            return Optional.empty();
        }
        return Optional.of(fit.shape());
    }
}
