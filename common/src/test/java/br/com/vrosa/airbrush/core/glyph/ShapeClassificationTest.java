package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.core.render.Curve;
import br.com.vrosa.airbrush.platform.Pose;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapeClassificationTest {

    private static final GlyphAnalyzer.Settings SETTINGS = new GlyphAnalyzer.Settings(0.6, 4.0);

    @Test
    void perfectCircleClassifiesWithHighQuality() {
        final var analysis = analyze(StrokeFactory.circle(StrokeFactory.floor(), 2.0, 120, 0, 0, 0, 1));
        assertShape(analysis, BaseShape.CIRCLE);
        assertTrue(analysis.glyph().orElseThrow().quality() > 0.95, "quality=" + analysis.glyph().orElseThrow().quality());
    }

    @Test
    void perfectSquareClassifiesWithHighQuality() {
        final var analysis = analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.squareVertices(1.5, 0), 120, 0, 0, 1));
        assertShape(analysis, BaseShape.SQUARE);
        assertTrue(analysis.glyph().orElseThrow().quality() > 0.95, "quality=" + analysis.glyph().orElseThrow().quality());
    }

    @Test
    void perfectTriangleClassifiesWithHighQuality() {
        final var analysis = analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.triangleVertices(2.0, 0), 120, 0, 0, 1));
        assertShape(analysis, BaseShape.TRIANGLE);
        assertTrue(analysis.glyph().orElseThrow().quality() > 0.95, "quality=" + analysis.glyph().orElseThrow().quality());
    }

    @Test
    void noisyShapesStillClassify() {
        assertShape(analyze(StrokeFactory.circle(StrokeFactory.floor(), 2.0, 120, 0, 0.10, 0, 7)),
                BaseShape.CIRCLE);
        assertShape(analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.squareVertices(1.5, 0), 120, 0.085, 0, 7)), BaseShape.SQUARE);
        assertShape(analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.triangleVertices(2.0, 0), 120, 0.075, 0, 7)), BaseShape.TRIANGLE);
    }

    @Test
    void chaikinSmoothedSquareStillClassifies() {
        final var raw = StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.squareVertices(1.5, 20), 100, 0, 0, 3);
        final var smoothed = Curve.chaikin(raw, 2);
        assertShape(analyze(smoothed), BaseShape.SQUARE);
    }

    @Test
    void inPlaneRotationsDoNotChangeClassification() {
        for (final double rotation : new double[]{30, 45, 77}) {
            assertShape(analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                    StrokeFactory.squareVertices(1.5, rotation), 120, 0.02, 0, 11)), BaseShape.SQUARE);
        }
    }

    @Test
    void wallAndRampDrawingsClassify() {
        assertShape(analyze(StrokeFactory.circle(StrokeFactory.wall(), 2.0, 120, 0, 0.02, 0, 13)),
                BaseShape.CIRCLE);
        assertShape(analyze(StrokeFactory.polygon(StrokeFactory.wall(),
                StrokeFactory.squareVertices(1.5, 30), 120, 0.02, 0, 13)), BaseShape.SQUARE);
        assertShape(analyze(StrokeFactory.polygon(StrokeFactory.ramp45(),
                StrokeFactory.triangleVertices(2.0, 15), 120, 0.02, 0, 13)), BaseShape.TRIANGLE);
    }

    @Test
    void largeClosureGapIsNotClosed() {
        final var analysis = analyze(StrokeFactory.circle(StrokeFactory.floor(), 2.0, 120, 0, 0, 0.3, 1));
        assertEquals(Reason.NOT_CLOSED, analysis.reason());
    }

    @Test
    void pentagonIsUnrecognized() {
        final var analysis = analyze(StrokeFactory.polygon(StrokeFactory.floor(),
                StrokeFactory.pentagonVertices(2.0, 0), 120, 0, 0, 1));
        assertEquals(Reason.UNRECOGNIZED_SHAPE, analysis.reason());
    }

    @Test
    void tinyShapeIsTooSmall() {
        final var analysis = analyze(StrokeFactory.circle(StrokeFactory.floor(), 0.3, 80, 0, 0, 0, 1));
        assertEquals(Reason.TOO_SMALL, analysis.reason());
    }

    @Test
    void hugeShapeIsTooLarge() {
        final var analysis = analyze(StrokeFactory.circle(StrokeFactory.floor(), 5.5, 160, 0, 0, 0, 1));
        assertEquals(Reason.TOO_LARGE, analysis.reason());
    }

    private static GlyphAnalysis analyze(List<Pose> samples) {
        return GlyphAnalyzer.analyze(List.of(StrokeFactory.stroke(samples)), SETTINGS);
    }

    private static void assertShape(GlyphAnalysis analysis, BaseShape expected) {
        assertEquals(Reason.OK, analysis.reason(), "reason=" + analysis.reason());
        assertEquals(expected, analysis.glyph().orElseThrow().base());
    }
}
