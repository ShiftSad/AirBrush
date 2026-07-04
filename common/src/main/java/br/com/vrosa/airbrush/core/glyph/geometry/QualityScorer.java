package br.com.vrosa.airbrush.core.glyph.geometry;

import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import org.jetbrains.annotations.NotNull;

public final class QualityScorer {

    private static final double CLOSURE_WEIGHT = 0.25;
    private static final double REGULARITY_WEIGHT = 0.35;
    private static final double SMOOTHNESS_WEIGHT = 0.20;
    private static final double CLEANLINESS_WEIGHT = 0.20;

    private static final double TREMOR_LIMIT_RADIANS = 0.8;
    private static final double ANGLE_TOLERANCE_DEGREES = 35.0;
    private static final double NOISE_PENALTY = 0.25;

    private QualityScorer() {}

    public record Breakdown(double total, double closure, double regularity,
                            double smoothness, double cleanliness) {}

    public static @NotNull Breakdown score(@NotNull ShapeClassifier.Fit fit, int noiseCount) {
        final double closure = 1.0 - clamp(fit.closureGap()
                / (ShapeClassifier.CLOSURE_PERIMETER_FRACTION * fit.perimeter()));
        final double regularity = regularity(fit);
        final double smoothness = 1.0 - clamp(fit.tremorRadians() / TREMOR_LIMIT_RADIANS);
        final double cleanliness = Math.max(0.0, 1.0 - NOISE_PENALTY * noiseCount);

        final double total = CLOSURE_WEIGHT * closure
                + REGULARITY_WEIGHT * regularity
                + SMOOTHNESS_WEIGHT * smoothness
                + CLEANLINESS_WEIGHT * cleanliness;
        return new Breakdown(total, closure, regularity, smoothness, cleanliness);
    }

    private static double regularity(@NotNull ShapeClassifier.Fit fit) {
        if (fit.shape() == BaseShape.CIRCLE) {
            final double deviation = fit.meanRadius() <= 0 ? 1.0 : fit.radiusDeviation() / fit.meanRadius();
            return 1.0 - clamp(deviation / 0.18);
        }

        double sideSum = 0;
        for (final double side : fit.sideLengths()) sideSum += side;
        final double sideMean = sideSum / fit.sideLengths().size();
        double sideVariance = 0;
        for (final double side : fit.sideLengths()) sideVariance += (side - sideMean) * (side - sideMean);
        final double sideUniformity = sideMean <= 0
                ? 0.0 : 1.0 - clamp(Math.sqrt(sideVariance / fit.sideLengths().size()) / sideMean);

        final double ideal = fit.shape() == BaseShape.SQUARE ? 90.0 : 60.0;
        double angleError = 0;
        for (final var corner : fit.corners()) angleError += Math.abs(corner.interiorDegrees() - ideal);
        angleError /= fit.corners().size();
        final double anglePrecision = 1.0 - clamp(angleError / ANGLE_TOLERANCE_DEGREES);

        return (sideUniformity + anglePrecision) / 2.0;
    }

    private static double clamp(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }
}
