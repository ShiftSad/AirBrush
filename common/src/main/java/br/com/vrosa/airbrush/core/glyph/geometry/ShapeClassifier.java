package br.com.vrosa.airbrush.core.glyph.geometry;

import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public final class ShapeClassifier {

    public static final int SAMPLES = 64;
    public static final int CORNER_WINDOW = 4;
    public static final double CLOSURE_PERIMETER_FRACTION = 0.15;
    public static final double CLOSURE_ABSOLUTE = 0.35;

    private static final double MIN_TURN_DEGREES = 40.0;
    private static final double CIRCLE_MAX_RADIUS_DEVIATION = 0.18;
    private static final int SMOOTHING_RADIUS = 2;

    private ShapeClassifier() {}

    public record Fit(@NotNull BaseShape shape, @NotNull List<Vector2d> ring, @NotNull Vector2d centroid,
                      double meanRadius, double perimeter, double closureGap,
                      @NotNull List<StrokeGeometry.Corner> corners,
                      double radiusDeviation, double tremorRadians, @NotNull List<Double> sideLengths) {}

    public record Outcome(boolean closed, @Nullable Fit fit, double perimeter, double closureGap) {}

    public static @NotNull Outcome classify(@NotNull List<Vector2d> points) {
        final double openLength = StrokeGeometry.length(points);
        final double gap = points.size() < 2 ? 0 : points.getFirst().distance(points.getLast());
        if (points.size() < 3 || openLength < 1.0e-6) return new Outcome(false, null, openLength, gap);

        final boolean closed = gap <= Math.max(CLOSURE_PERIMETER_FRACTION * openLength, CLOSURE_ABSOLUTE);
        if (!closed) return new Outcome(false, null, openLength, gap);

        final var rawRing = StrokeGeometry.resampleClosed(points, SAMPLES);
        final var ring = StrokeGeometry.smoothRing(rawRing, SMOOTHING_RADIUS);
        final double perimeter = StrokeGeometry.length(rawRing) + rawRing.getLast().distance(rawRing.getFirst());
        final var centroid = StrokeGeometry.centroid(ring);
        final double meanRadius = StrokeGeometry.meanRadius(rawRing, centroid);
        final double radiusDeviation = StrokeGeometry.radiusDeviation(rawRing, centroid, meanRadius);

        final double[] smoothTurns = StrokeGeometry.turnAnglesDegrees(ring, CORNER_WINDOW);
        final var corners = refineCorners(ring,
                StrokeGeometry.corners(ring, smoothTurns, MIN_TURN_DEGREES));

        final var shape = matchShape(corners, radiusDeviation, meanRadius);
        if (shape == null) return new Outcome(true, null, perimeter, gap);

        final double[] rawTurns = StrokeGeometry.turnAnglesDegrees(rawRing, CORNER_WINDOW);
        final double tremor = tremorRadians(rawTurns, corners, shape);
        final var sides = sideLengths(corners, perimeter);
        return new Outcome(true, new Fit(shape, ring, centroid, meanRadius, perimeter, gap,
                corners, radiusDeviation, tremor, sides), perimeter, gap);
    }

    private static @NotNull List<StrokeGeometry.Corner> refineCorners(@NotNull List<Vector2d> ring,
                                                                      @NotNull List<StrokeGeometry.Corner> detected) {
        final int count = detected.size();
        if (count < 2) return detected;

        final int n = ring.size();
        final var sideDirections = new Vector2d[count];
        for (int j = 0; j < count; j++) {
            final int from = detected.get(j).index();
            final int to = detected.get((j + 1) % count).index();
            final int span = Math.floorMod(to - from, n);
            final int trim = span > CORNER_WINDOW * 2 + 1 ? CORNER_WINDOW : 0;
            final var a = ring.get(Math.floorMod(from + trim, n));
            final var b = ring.get(Math.floorMod(to - trim, n));
            sideDirections[j] = new Vector2d(b).sub(a);
        }

        final var refined = new ArrayList<StrokeGeometry.Corner>(count);
        for (int j = 0; j < count; j++) {
            final var incoming = sideDirections[Math.floorMod(j - 1, count)];
            final var outgoing = sideDirections[j];
            final double turn = Math.toDegrees(StrokeGeometry.angleBetween(
                    incoming.x, incoming.y, outgoing.x, outgoing.y));
            refined.add(new StrokeGeometry.Corner(detected.get(j).index(), detected.get(j).position(), turn));
        }
        return refined;
    }

    private static @Nullable BaseShape matchShape(@NotNull List<StrokeGeometry.Corner> corners,
                                                  double radiusDeviation, double meanRadius) {
        if (corners.isEmpty()) {
            return meanRadius > 0 && radiusDeviation / meanRadius < CIRCLE_MAX_RADIUS_DEVIATION
                    ? BaseShape.CIRCLE : null;
        }
        if (corners.size() == 3) {
            double turnSum = 0;
            for (final var corner : corners) {
                if (corner.interiorDegrees() < 35 || corner.interiorDegrees() > 110) return null;
                turnSum += corner.turnDegrees();
            }
            return turnSum >= 288 && turnSum <= 432 ? BaseShape.TRIANGLE : null;
        }
        if (corners.size() == 4) {
            for (final var corner : corners) {
                if (corner.interiorDegrees() < 55 || corner.interiorDegrees() > 115) return null;
            }
            return BaseShape.SQUARE;
        }
        return null;
    }

    private static double tremorRadians(double @NotNull [] turns,
                                        @NotNull List<StrokeGeometry.Corner> corners,
                                        @NotNull BaseShape shape) {
        final int n = turns.length;
        double reference = 0;
        if (shape == BaseShape.CIRCLE) {
            for (final double turn : turns) reference += turn;
            reference /= n;
        }

        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (nearCorner(i, corners, n)) continue;
            sum += Math.abs(Math.toRadians(turns[i] - reference));
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    private static boolean nearCorner(int index, @NotNull List<StrokeGeometry.Corner> corners, int n) {
        for (final var corner : corners) {
            final int direct = Math.abs(index - corner.index());
            if (Math.min(direct, n - direct) <= CORNER_WINDOW) return true;
        }
        return false;
    }

    private static @NotNull List<Double> sideLengths(@NotNull List<StrokeGeometry.Corner> corners, double perimeter) {
        if (corners.isEmpty()) return List.of();
        final var sides = new ArrayList<Double>(corners.size());
        for (int i = 0; i < corners.size(); i++) {
            final int from = corners.get(i).index();
            final int to = corners.get((i + 1) % corners.size()).index();
            final int span = Math.floorMod(to - from, SAMPLES);
            sides.add(span * perimeter / SAMPLES);
        }
        return sides;
    }
}
