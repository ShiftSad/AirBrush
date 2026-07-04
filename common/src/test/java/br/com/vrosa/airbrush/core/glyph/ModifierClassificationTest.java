package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.platform.Pose;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierClassificationTest {

    private static final GlyphAnalyzer.Settings SETTINGS = new GlyphAnalyzer.Settings(0.6, 4.0);
    private static final StrokeFactory.Basis FLOOR = StrokeFactory.floor();

    @Test
    void focusDotDetectedOnEveryBase() {
        final var dot = StrokeFactory.circle(FLOOR, 0.1, 24, 0, 0, 0, 5);
        for (final var base : bases()) {
            final var analysis = analyze(base, dot);
            assertTrue(analysis.glyph().orElseThrow().modifiers().contains(Modifier.FOCUS_DOT),
                    "base=" + analysis.glyph().orElseThrow().base());
        }
    }

    @Test
    void smallDrawnCircleAtCenterIsFocusDot() {
        final var analysis = analyze(circleBase(), StrokeFactory.circle(FLOOR, 0.55, 40, 0, 0.01, 0, 5));
        assertTrue(analysis.glyph().orElseThrow().modifiers().contains(Modifier.FOCUS_DOT));
    }

    @Test
    void divisionLineDetectedOnEveryBase() {
        final var line = StrokeFactory.line(FLOOR, new Vector2d(-2.6, 0.05), new Vector2d(2.6, -0.05), 40, 0.005, 5);
        for (final var base : bases()) {
            final var analysis = analyze(base, line);
            assertTrue(analysis.glyph().orElseThrow().modifiers().contains(Modifier.DIVISION_LINE),
                    "base=" + analysis.glyph().orElseThrow().base());
        }
    }

    @Test
    void dividerDrawnEdgeToEdgeInsideIsDivisionLine() {
        final var triangle = StrokeFactory.polygon(FLOOR, StrokeFactory.triangleVertices(2.2, 0), 120, 0, 0, 1);
        final var median = StrokeFactory.line(FLOOR, new Vector2d(2.15, 0.02), new Vector2d(-1.05, -0.02), 40, 0.01, 5);
        final var analysis = analyze(triangle, median);
        assertTrue(analysis.glyph().orElseThrow().modifiers().contains(Modifier.DIVISION_LINE));
    }

    @Test
    void eccentricLineIsNotDivisionLine() {
        final var line = StrokeFactory.line(FLOOR, new Vector2d(-2.6, 1.2), new Vector2d(2.6, 1.2), 40, 0.005, 5);
        final var analysis = analyze(circleBase(), line);
        assertFalse(analysis.glyph().orElseThrow().modifiers().contains(Modifier.DIVISION_LINE));
    }

    @Test
    void raysDetectedOnEveryBase() {
        for (final var base : bases()) {
            final var strokes = new ArrayList<List<Pose>>();
            strokes.add(base);
            for (final var ray : raysAround(base)) strokes.add(ray);

            final var analysis = analyzeAll(strokes);
            assertTrue(analysis.glyph().orElseThrow().modifiers().contains(Modifier.RAYS),
                    "base=" + analysis.glyph().orElseThrow().base());
        }
    }

    @Test
    void extraClosedShapesAreNotModifiers() {
        final var triangleInCircle = analyze(circleBase(),
                StrokeFactory.polygon(FLOOR, StrokeFactory.triangleVertices(0.9, 10), 80, 0, 0, 5));
        assertTrue(triangleInCircle.glyph().orElseThrow().modifiers().isEmpty());

        final var circles = analyze(circleBase(), StrokeFactory.circle(FLOOR, 1.2, 80, 0, 0, 0, 5));
        assertEquals(BaseShape.CIRCLE, circles.glyph().orElseThrow().base());
        assertTrue(circles.glyph().orElseThrow().modifiers().isEmpty());
    }

    private static List<List<Pose>> raysAround(List<Pose> base) {
        final var center = new Vector2d();
        final var rays = new ArrayList<List<Pose>>();
        final var ring = ringOf(base);
        for (int i = 0; i < 4; i++) {
            final var anchor = ring.get(ring.size() * i / 4);
            final var direction = new Vector2d(anchor).sub(center).normalize();
            final var from = new Vector2d(anchor).fma(0.05, direction);
            final var to = new Vector2d(anchor).fma(0.55, direction);
            rays.add(StrokeFactory.line(FLOOR, from, to, 12, 0.005, 17 + i));
        }
        return rays;
    }

    private static List<Vector2d> ringOf(List<Pose> base) {
        final var origin = FLOOR.origin();
        final var u = FLOOR.u();
        final var v = FLOOR.v();
        final var result = new ArrayList<Vector2d>(base.size());
        for (final var pose : base) {
            final double dx = pose.position().x() - origin.x;
            final double dy = pose.position().y() - origin.y;
            final double dz = pose.position().z() - origin.z;
            result.add(new Vector2d(
                    dx * u.x + dy * u.y + dz * u.z,
                    dx * v.x + dy * v.y + dz * v.z));
        }
        return result;
    }

    private static List<Pose> circleBase() {
        return StrokeFactory.circle(FLOOR, 2.0, 120, 0, 0, 0, 1);
    }

    private static List<List<Pose>> bases() {
        return List.of(
                circleBase(),
                StrokeFactory.polygon(FLOOR, StrokeFactory.squareVertices(1.7, 0), 120, 0, 0, 1),
                StrokeFactory.polygon(FLOOR, StrokeFactory.triangleVertices(2.2, 0), 120, 0, 0, 1));
    }

    private static GlyphAnalysis analyze(List<Pose> base, List<Pose> extra) {
        return analyzeAll(List.of(base, extra));
    }

    private static GlyphAnalysis analyzeAll(List<List<Pose>> sampleLists) {
        final var strokes = new ArrayList<GlyphStroke>(sampleLists.size());
        for (final var samples : sampleLists) strokes.add(StrokeFactory.stroke(samples));
        final var analysis = GlyphAnalyzer.analyze(strokes, SETTINGS);
        assertEquals(Reason.OK, analysis.reason());
        return analysis;
    }
}
