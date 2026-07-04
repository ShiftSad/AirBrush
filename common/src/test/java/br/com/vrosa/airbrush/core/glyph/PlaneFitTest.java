package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.geometry.StrokeGeometry;
import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaneFitTest {

    private static final GlyphAnalyzer.Settings SETTINGS = new GlyphAnalyzer.Settings(0.6, 4.0);

    @Test
    void recoversNormalOfTiltedPlane() {
        final var basis = StrokeFactory.ramp45();
        final var samples = StrokeFactory.circle(basis, 2.0, 100, 0, 0.01, 0, 42);

        final var points = positions(samples);
        final var plane = StrokeGeometry.fitPlane(points);

        final double alignment = Math.abs(new Vector3d(plane.normal()).dot(basis.normal()));
        assertTrue(alignment > 0.99, "alignment=" + alignment);
        assertTrue(StrokeGeometry.planarityRms(points, plane) < 0.05);
    }

    @Test
    void diagonalCovarianceSelectsSmallestAxis() {
        final var normal = StrokeGeometry.smallestEigenvector(4.0, 0, 0, 2.0, 0, 9.0);
        assertEquals(1.0, Math.abs(normal.y), 1.0e-9);
    }

    @Test
    void helixIsNotPlanar() {
        final var samples = StrokeFactory.helix(StrokeFactory.floor(), 1.5, 3.0, 100);
        final var analysis = GlyphAnalyzer.analyze(List.of(StrokeFactory.stroke(samples)), SETTINGS);
        assertEquals(Reason.NOT_PLANAR, analysis.reason());
    }

    private static List<Vec3> positions(List<Pose> samples) {
        final var points = new ArrayList<Vec3>(samples.size());
        for (final var sample : samples) points.add(sample.position());
        return points;
    }
}
