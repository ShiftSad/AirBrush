package br.com.vrosa.airbrush.core.glyph.geometry;

import br.com.vrosa.airbrush.core.glyph.GlyphStroke;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import br.com.vrosa.airbrush.core.glyph.model.Stroke2D;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ModifierClassifier {

    private static final double FOCUS_DOT_MAX_DISTANCE_FACTOR = 0.4;
    private static final double DIVISION_MAX_CENTER_DISTANCE_FACTOR = 0.35;
    private static final double DIVISION_MIN_LENGTH_FACTOR = 1.2;
    private static final double DIVISION_MAX_END_RING_DISTANCE = 0.4;
    private static final double STRAIGHT_MAX_DEVIATION_RATIO = 0.12;
    private static final double RAY_MAX_PERIMETER = 1.5;
    private static final double RAY_MAX_RING_DISTANCE = 0.4;
    private static final double RAY_MAX_RADIAL_DEGREES = 30.0;
    private static final int RAYS_MIN_COUNT = 3;

    private ModifierClassifier() {}

    public record Result(@NotNull Set<Modifier> modifiers, @NotNull List<GlyphStroke> matched, int noiseCount) {}

    public static @NotNull Result classify(@NotNull ShapeClassifier.Fit base, @NotNull List<Stroke2D> strokes) {
        final var modifiers = EnumSet.noneOf(Modifier.class);
        final var matched = new ArrayList<GlyphStroke>();
        final var rayCandidates = new ArrayList<Stroke2D>();
        int noise = 0;

        for (final var stroke : strokes) {
            final var points = stroke.points();
            if (isFocusDot(base, points)) {
                modifiers.add(Modifier.FOCUS_DOT);
                matched.add(stroke.source());
                continue;
            }

            final var outcome = ShapeClassifier.classify(points);
            if (outcome.closed()) {
                noise++;
                continue;
            }

            if (isDivisionLine(base, points, outcome.perimeter())) {
                modifiers.add(Modifier.DIVISION_LINE);
                matched.add(stroke.source());
            } else if (outcome.perimeter() < RAY_MAX_PERIMETER) {
                rayCandidates.add(stroke);
            } else {
                noise++;
            }
        }

        final var rays = new ArrayList<Stroke2D>();
        for (final var candidate : rayCandidates) {
            if (isRay(base, candidate.points())) rays.add(candidate);
            else noise++;
        }
        if (rays.size() >= RAYS_MIN_COUNT) {
            modifiers.add(Modifier.RAYS);
            for (final var ray : rays) matched.add(ray.source());
        } else {
            noise += rays.size();
        }

        return new Result(modifiers, matched, noise);
    }

    private static boolean isFocusDot(@NotNull ShapeClassifier.Fit base, @NotNull List<Vector2d> points) {
        final double maxDistance = FOCUS_DOT_MAX_DISTANCE_FACTOR * base.meanRadius();
        for (final var point : points) {
            if (point.distance(base.centroid()) >= maxDistance) return false;
        }
        return true;
    }

    private static boolean isDivisionLine(@NotNull ShapeClassifier.Fit base,
                                          @NotNull List<Vector2d> points, double perimeter) {
        if (points.size() < 2 || perimeter < DIVISION_MIN_LENGTH_FACTOR * base.meanRadius()) return false;
        final var line = StrokeGeometry.fitLine(points);
        if (StrokeGeometry.maxLineDeviation(points, line) / perimeter >= STRAIGHT_MAX_DEVIATION_RATIO) return false;
        if (line.distance(base.centroid()) >= DIVISION_MAX_CENTER_DISTANCE_FACTOR * base.meanRadius()) return false;

        if (StrokeGeometry.boundaryCrossings(points, base.ring()) == 2) return true;
        return reachesBoundary(base, points.getFirst()) && reachesBoundary(base, points.getLast());
    }

    private static boolean reachesBoundary(@NotNull ShapeClassifier.Fit base, @NotNull Vector2d end) {
        return !StrokeGeometry.pointInPolygon(end, base.ring())
                || StrokeGeometry.distanceToRing(end, base.ring()) < DIVISION_MAX_END_RING_DISTANCE;
    }

    private static boolean isRay(@NotNull ShapeClassifier.Fit base, @NotNull List<Vector2d> points) {
        if (points.size() < 2) return false;

        double ringDistance = Double.MAX_VALUE;
        for (final var point : points) {
            ringDistance = Math.min(ringDistance, StrokeGeometry.distanceToRing(point, base.ring()));
        }
        if (ringDistance >= RAY_MAX_RING_DISTANCE) return false;

        final var line = StrokeGeometry.fitLine(points);
        final var centroid = StrokeGeometry.centroid(points);
        final double radialX = centroid.x - base.centroid().x;
        final double radialY = centroid.y - base.centroid().y;
        double degrees = Math.toDegrees(StrokeGeometry.angleBetween(
                line.direction().x, line.direction().y, radialX, radialY));
        degrees = Math.min(degrees, 180.0 - degrees);
        return degrees < RAY_MAX_RADIAL_DEGREES;
    }
}
