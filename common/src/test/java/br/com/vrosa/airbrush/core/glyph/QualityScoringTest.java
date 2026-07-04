package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.platform.Pose;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityScoringTest {

    private static final GlyphAnalyzer.Settings SETTINGS = new GlyphAnalyzer.Settings(0.6, 4.0);
    private static final StrokeFactory.Basis FLOOR = StrokeFactory.floor();
    private static final double RADIUS = 2.0;

    @Test
    void lightNoiseScoresHigh() {
        assertTrue(qualityAt(0.02) >= 0.9, "quality=" + qualityAt(0.02));
    }

    @Test
    void heavyNoiseFailsOrScoresLow() {
        final var analysis = analyzeCircle(0.20);
        final double quality = analysis.glyph().map(g -> g.quality()).orElse(0.0);
        assertTrue(analysis.glyph().isEmpty() || quality < 0.4, "quality=" + quality);
    }

    @Test
    void moreNoiseNeverScoresHigher() {
        final double low = qualityAt(0.02);
        final double mid = qualityAt(0.06);
        final double high = qualityAt(0.12);
        assertTrue(low >= mid, "low=" + low + " mid=" + mid);
        assertTrue(mid >= high, "mid=" + mid + " high=" + high);
    }

    @Test
    void scribblesOnlyHurtCleanliness() {
        final var strokes = new ArrayList<List<Pose>>();
        strokes.add(StrokeFactory.polygon(FLOOR, StrokeFactory.squareVertices(1.5, 0), 120, 0, 0, 1));
        for (int i = 0; i < 4; i++) {
            final var at = new Vector2d(4.0 + i * 0.4, 3.5);
            strokes.add(StrokeFactory.line(FLOOR, at, new Vector2d(at.x + 0.4, at.y + 0.3), 8, 0.05, 31 + i));
        }

        final var glyphStrokes = new ArrayList<GlyphStroke>();
        for (final var samples : strokes) glyphStrokes.add(StrokeFactory.stroke(samples));
        final var analysis = GlyphAnalyzer.analyze(glyphStrokes, SETTINGS);

        assertEquals(Reason.OK, analysis.reason());
        final var breakdown = analysis.breakdown();
        assertEquals(0.0, breakdown.cleanliness(), 1.0e-9);
        assertTrue(breakdown.closure() > 0.8, "closure=" + breakdown.closure());
        assertTrue(breakdown.regularity() > 0.8, "regularity=" + breakdown.regularity());
        assertTrue(breakdown.smoothness() > 0.8, "smoothness=" + breakdown.smoothness());
    }

    private static double qualityAt(double noiseFraction) {
        final var analysis = analyzeCircle(noiseFraction);
        return analysis.glyph().map(g -> g.quality()).orElse(0.0);
    }

    private static GlyphAnalysis analyzeCircle(double noiseFraction) {
        final var samples = StrokeFactory.circle(FLOOR, RADIUS, 120, 0, noiseFraction * RADIUS, 0, 99);
        return GlyphAnalyzer.analyze(List.of(StrokeFactory.stroke(samples)), SETTINGS);
    }
}
