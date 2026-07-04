package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

final class StrokeFactory {

    record TestWorld(String id) implements WorldRef {}

    static final WorldRef WORLD = new TestWorld("test");

    record Basis(Vector3d origin, Vector3d u, Vector3d v) {

        Vector3d normal() {
            return u.cross(v, new Vector3d()).normalize();
        }

        Pose pose(double x, double y) {
            final var p = new Vector3d(origin).fma(x, u).fma(y, v);
            final var n = normal();
            return new Pose(WORLD, new Vec3(p.x, p.y, p.z), new Vector3f((float) n.x, (float) n.y, (float) n.z));
        }
    }

    private StrokeFactory() {}

    static Basis floor() {
        return new Basis(new Vector3d(10, 64, -5), new Vector3d(1, 0, 0), new Vector3d(0, 0, 1));
    }

    static Basis wall() {
        return new Basis(new Vector3d(-3, 70, 8), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0));
    }

    static Basis ramp45() {
        final double s = Math.sqrt(0.5);
        return new Basis(new Vector3d(0, 65, 0), new Vector3d(1, 0, 0), new Vector3d(0, s, s));
    }

    static List<Vector2d> squareVertices(double halfSide, double rotationDeg) {
        return rotate(List.of(
                new Vector2d(-halfSide, -halfSide),
                new Vector2d(halfSide, -halfSide),
                new Vector2d(halfSide, halfSide),
                new Vector2d(-halfSide, halfSide)), rotationDeg);
    }

    static List<Vector2d> triangleVertices(double circumradius, double rotationDeg) {
        return regularVertices(3, circumradius, rotationDeg);
    }

    static List<Vector2d> pentagonVertices(double circumradius, double rotationDeg) {
        return regularVertices(5, circumradius, rotationDeg);
    }

    static List<Pose> circle(Basis basis, double radius, int n, double rotationDeg,
                             double noiseSigma, double gapFraction, long seed) {
        final var random = new Random(seed);
        final var result = new ArrayList<Pose>(n);
        final double start = Math.toRadians(rotationDeg);
        final double sweep = 2 * Math.PI * (1 - gapFraction);
        for (int i = 0; i < n; i++) {
            final double t = start + sweep * i / n;
            final double x = radius * Math.cos(t) + random.nextGaussian() * noiseSigma;
            final double y = radius * Math.sin(t) + random.nextGaussian() * noiseSigma;
            result.add(basis.pose(x, y));
        }
        return result;
    }

    static List<Pose> polygon(Basis basis, List<Vector2d> vertices, int n,
                              double noiseSigma, double gapFraction, long seed) {
        final var random = new Random(seed);
        final double perimeter = ringLength(vertices);
        final double walk = perimeter * (1 - gapFraction);

        final var result = new ArrayList<Pose>(n);
        for (int i = 0; i < n; i++) {
            final var p = pointAt(vertices, walk * i / n);
            result.add(basis.pose(
                    p.x + random.nextGaussian() * noiseSigma,
                    p.y + random.nextGaussian() * noiseSigma));
        }
        return result;
    }

    static List<Pose> line(Basis basis, Vector2d from, Vector2d to, int n, double noiseSigma, long seed) {
        final var random = new Random(seed);
        final var result = new ArrayList<Pose>(n);
        for (int i = 0; i < n; i++) {
            final double t = (double) i / (n - 1);
            result.add(basis.pose(
                    from.x + (to.x - from.x) * t + random.nextGaussian() * noiseSigma,
                    from.y + (to.y - from.y) * t + random.nextGaussian() * noiseSigma));
        }
        return result;
    }

    static List<Pose> helix(Basis basis, double radius, double rise, int n) {
        final var result = new ArrayList<Pose>(n);
        for (int i = 0; i < n; i++) {
            final double t = 2 * Math.PI * i / n;
            final var inPlane = basis.pose(radius * Math.cos(t), radius * Math.sin(t));
            final var lift = basis.normal().mul(rise * i / n);
            result.add(new Pose(WORLD, new Vec3(
                    inPlane.position().x() + lift.x,
                    inPlane.position().y() + lift.y,
                    inPlane.position().z() + lift.z), inPlane.normal()));
        }
        return result;
    }

    static GlyphStroke stroke(List<Pose> samples) {
        return new GlyphStroke(UUID.randomUUID(), UUID.randomUUID(), WORLD, samples,
                List.of(new FakeSegmentHandle()), Long.MAX_VALUE);
    }

    static List<GlyphStroke> strokes(List<Pose>... sampleLists) {
        final var result = new ArrayList<GlyphStroke>(sampleLists.length);
        for (final var samples : sampleLists) result.add(stroke(samples));
        return result;
    }

    private static List<Vector2d> regularVertices(int sides, double circumradius, double rotationDeg) {
        final var vertices = new ArrayList<Vector2d>(sides);
        for (int i = 0; i < sides; i++) {
            final double angle = Math.toRadians(rotationDeg) + 2 * Math.PI * i / sides;
            vertices.add(new Vector2d(circumradius * Math.cos(angle), circumradius * Math.sin(angle)));
        }
        return vertices;
    }

    private static List<Vector2d> rotate(List<Vector2d> points, double rotationDeg) {
        final double angle = Math.toRadians(rotationDeg);
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        final var result = new ArrayList<Vector2d>(points.size());
        for (final var p : points) result.add(new Vector2d(p.x * cos - p.y * sin, p.x * sin + p.y * cos));
        return result;
    }

    private static double ringLength(List<Vector2d> vertices) {
        double total = 0;
        for (int i = 0; i < vertices.size(); i++) {
            total += vertices.get(i).distance(vertices.get((i + 1) % vertices.size()));
        }
        return total;
    }

    private static Vector2d pointAt(List<Vector2d> vertices, double distance) {
        double remaining = distance;
        for (int i = 0; ; i = (i + 1) % vertices.size()) {
            final var from = vertices.get(i);
            final var to = vertices.get((i + 1) % vertices.size());
            final double edge = from.distance(to);
            if (remaining <= edge || edge == 0) {
                final double t = edge == 0 ? 0 : remaining / edge;
                return new Vector2d(from).lerp(to, t);
            }
            remaining -= edge;
        }
    }
}
