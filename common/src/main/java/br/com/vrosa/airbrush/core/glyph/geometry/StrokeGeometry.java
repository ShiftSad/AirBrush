package br.com.vrosa.airbrush.core.glyph.geometry;

import br.com.vrosa.airbrush.platform.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public final class StrokeGeometry {

    private static final double EPSILON = 1.0e-9;

    private StrokeGeometry() {}

    public record Plane(@NotNull Vector3d origin, @NotNull Vector3d normal,
                        @NotNull Vector3d u, @NotNull Vector3d v) {

        public @NotNull Vector2d project(@NotNull Vec3 point) {
            final var d = new Vector3d(point.x(), point.y(), point.z()).sub(origin);
            return new Vector2d(d.dot(u), d.dot(v));
        }

        public @NotNull Vec3 unproject(@NotNull Vector2d point) {
            final var p = new Vector3d(origin)
                    .fma(point.x, u)
                    .fma(point.y, v);
            return new Vec3(p.x, p.y, p.z);
        }

        public double distance(@NotNull Vec3 point) {
            return new Vector3d(point.x(), point.y(), point.z()).sub(origin).dot(normal);
        }
    }

    public record Corner(int index, @NotNull Vector2d position, double turnDegrees) {

        public double interiorDegrees() {
            return 180.0 - turnDegrees;
        }
    }

    public record Line(@NotNull Vector2d point, @NotNull Vector2d direction) {

        public double distance(@NotNull Vector2d p) {
            final double dx = p.x - point.x;
            final double dy = p.y - point.y;
            return Math.abs(dx * direction.y - dy * direction.x);
        }
    }

    public static @NotNull Plane fitPlane(@NotNull List<Vec3> points) {
        final var origin = centroid3(points);

        double xx = 0, xy = 0, xz = 0, yy = 0, yz = 0, zz = 0;
        for (final var p : points) {
            final double dx = p.x() - origin.x;
            final double dy = p.y() - origin.y;
            final double dz = p.z() - origin.z;
            xx += dx * dx;
            xy += dx * dy;
            xz += dx * dz;
            yy += dy * dy;
            yz += dy * dz;
            zz += dz * dz;
        }

        final var normal = smallestEigenvector(xx, xy, xz, yy, yz, zz);
        final var reference = Math.abs(normal.y) < 0.9 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
        final var u = reference.cross(normal, new Vector3d()).normalize();
        final var v = new Vector3d(normal).cross(u).normalize();
        return new Plane(origin, normal, u, v);
    }

    public static double planarityRms(@NotNull List<Vec3> points, @NotNull Plane plane) {
        if (points.isEmpty()) return 0;
        double sum = 0;
        for (final var p : points) {
            final double d = plane.distance(p);
            sum += d * d;
        }
        return Math.sqrt(sum / points.size());
    }

    public static @NotNull Vector3d smallestEigenvector(double xx, double xy, double xz,
                                                        double yy, double yz, double zz) {
        final double p1 = xy * xy + xz * xz + yz * yz;
        if (p1 < EPSILON) {
            if (xx <= yy && xx <= zz) return new Vector3d(1, 0, 0);
            if (yy <= zz) return new Vector3d(0, 1, 0);
            return new Vector3d(0, 0, 1);
        }

        final double q = (xx + yy + zz) / 3.0;
        final double p2 = (xx - q) * (xx - q) + (yy - q) * (yy - q) + (zz - q) * (zz - q) + 2.0 * p1;
        final double p = Math.sqrt(p2 / 6.0);

        final double bxx = (xx - q) / p, bxy = xy / p, bxz = xz / p;
        final double byy = (yy - q) / p, byz = yz / p;
        final double bzz = (zz - q) / p;
        final double det = bxx * (byy * bzz - byz * byz)
                - bxy * (bxy * bzz - byz * bxz)
                + bxz * (bxy * byz - byy * bxz);

        final double phi = Math.acos(Math.clamp(det / 2.0, -1.0, 1.0)) / 3.0;
        final double smallest = q + 2.0 * p * Math.cos(phi + 2.0 * Math.PI / 3.0);

        final var row0 = new Vector3d(xx - smallest, xy, xz);
        final var row1 = new Vector3d(xy, yy - smallest, yz);
        final var row2 = new Vector3d(xz, yz, zz - smallest);

        var best = row0.cross(row1, new Vector3d());
        var candidate = row0.cross(row2, new Vector3d());
        if (candidate.lengthSquared() > best.lengthSquared()) best = candidate;
        candidate = row1.cross(row2, new Vector3d());
        if (candidate.lengthSquared() > best.lengthSquared()) best = candidate;

        if (best.lengthSquared() < EPSILON) {
            final var axis = row0.lengthSquared() >= Math.max(row1.lengthSquared(), row2.lengthSquared())
                    ? row0 : (row1.lengthSquared() >= row2.lengthSquared() ? row1 : row2);
            if (axis.lengthSquared() < EPSILON) return new Vector3d(0, 1, 0);
            final var reference = Math.abs(axis.y) < 0.9 * axis.length() ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
            return axis.cross(reference, new Vector3d()).normalize();
        }
        return best.normalize();
    }

    public static double length(@NotNull List<Vector2d> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distance(points.get(i));
        return total;
    }

    public static @NotNull Vector2d centroid(@NotNull List<Vector2d> points) {
        final var c = new Vector2d();
        for (final var p : points) c.add(p);
        return c.div(Math.max(1, points.size()));
    }

    public static double meanRadius(@NotNull List<Vector2d> points, @NotNull Vector2d center) {
        double sum = 0;
        for (final var p : points) sum += p.distance(center);
        return sum / Math.max(1, points.size());
    }

    public static double radiusDeviation(@NotNull List<Vector2d> points, @NotNull Vector2d center, double meanRadius) {
        double sum = 0;
        for (final var p : points) {
            final double d = p.distance(center) - meanRadius;
            sum += d * d;
        }
        return Math.sqrt(sum / Math.max(1, points.size()));
    }

    public static @NotNull List<Vector2d> resampleClosed(@NotNull List<Vector2d> points, int samples) {
        final var loop = new ArrayList<>(points);
        loop.add(points.getFirst());
        final double perimeter = length(loop);
        if (perimeter < EPSILON) return uniformCopies(points.getFirst(), samples);

        final var result = new ArrayList<Vector2d>(samples);
        final double step = perimeter / samples;
        double targetDistance = 0;
        double walked = 0;
        int edge = 0;
        var from = loop.get(0);
        var to = loop.get(1);
        double edgeLength = from.distance(to);

        for (int i = 0; i < samples; i++) {
            while (walked + edgeLength < targetDistance && edge < loop.size() - 2) {
                walked += edgeLength;
                edge++;
                from = loop.get(edge);
                to = loop.get(edge + 1);
                edgeLength = from.distance(to);
            }
            final double t = edgeLength < EPSILON ? 0 : (targetDistance - walked) / edgeLength;
            result.add(new Vector2d(from).lerp(to, Math.clamp(t, 0.0, 1.0)));
            targetDistance += step;
        }
        return result;
    }

    public static @NotNull List<Vector2d> smoothRing(@NotNull List<Vector2d> ring, int radius) {
        final int n = ring.size();
        if (n < radius * 2 + 1) return ring;

        final var result = new ArrayList<Vector2d>(n);
        for (int i = 0; i < n; i++) {
            final var average = new Vector2d();
            for (int k = -radius; k <= radius; k++) average.add(ring.get(Math.floorMod(i + k, n)));
            result.add(average.div(radius * 2 + 1));
        }
        return result;
    }

    public static double @NotNull [] turnAnglesDegrees(@NotNull List<Vector2d> ring, int window) {
        final int n = ring.size();
        final double[] turns = new double[n];
        if (n < window * 2 + 1) return turns;

        for (int i = 0; i < n; i++) {
            final var before = ring.get(Math.floorMod(i - window, n));
            final var at = ring.get(i);
            final var after = ring.get(Math.floorMod(i + window, n));
            turns[i] = Math.toDegrees(angleBetween(
                    at.x - before.x, at.y - before.y,
                    after.x - at.x, after.y - at.y));
        }
        return turns;
    }

    public static @NotNull List<Corner> corners(@NotNull List<Vector2d> ring, double @NotNull [] turns,
                                                double minTurnDegrees) {
        final int n = ring.size();
        final var candidates = new ArrayList<Integer>();
        for (int i = 0; i < n; i++) {
            if (turns[i] <= minTurnDegrees) continue;
            final double prev = turns[Math.floorMod(i - 1, n)];
            final double next = turns[Math.floorMod(i + 1, n)];
            if (turns[i] >= prev && turns[i] > next) candidates.add(i);
        }
        candidates.sort((a, b) -> Double.compare(turns[b], turns[a]));

        final int minSeparation = n / 12;
        final var kept = new ArrayList<Integer>();
        for (final int candidate : candidates) {
            boolean suppressed = false;
            for (final int existing : kept) {
                final int direct = Math.abs(candidate - existing);
                if (Math.min(direct, n - direct) < minSeparation) {
                    suppressed = true;
                    break;
                }
            }
            if (!suppressed) kept.add(candidate);
        }
        kept.sort(Integer::compare);

        final var result = new ArrayList<Corner>(kept.size());
        for (final int index : kept) result.add(new Corner(index, new Vector2d(ring.get(index)), turns[index]));
        return result;
    }

    public static boolean pointInPolygon(@NotNull Vector2d point, @NotNull List<Vector2d> ring) {
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            final var a = ring.get(i);
            final var b = ring.get(j);
            if ((a.y > point.y) != (b.y > point.y)
                    && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) {
                inside = !inside;
            }
        }
        return inside;
    }

    public static @NotNull Line fitLine(@NotNull List<Vector2d> points) {
        final var center = centroid(points);
        double xx = 0, xy = 0, yy = 0;
        for (final var p : points) {
            final double dx = p.x - center.x;
            final double dy = p.y - center.y;
            xx += dx * dx;
            xy += dx * dy;
            yy += dy * dy;
        }

        final double trace = xx + yy;
        final double det = xx * yy - xy * xy;
        final double largest = trace / 2.0 + Math.sqrt(Math.max(0, trace * trace / 4.0 - det));
        var direction = Math.abs(xy) > EPSILON
                ? new Vector2d(largest - yy, xy)
                : (xx >= yy ? new Vector2d(1, 0) : new Vector2d(0, 1));
        if (direction.lengthSquared() < EPSILON) direction = new Vector2d(1, 0);
        return new Line(center, direction.normalize());
    }

    public static double maxLineDeviation(@NotNull List<Vector2d> points, @NotNull Line line) {
        double max = 0;
        for (final var p : points) max = Math.max(max, line.distance(p));
        return max;
    }

    public static int boundaryCrossings(@NotNull List<Vector2d> polyline, @NotNull List<Vector2d> ring) {
        int crossings = 0;
        for (int i = 1; i < polyline.size(); i++) {
            final var a = polyline.get(i - 1);
            final var b = polyline.get(i);
            for (int j = 0, k = ring.size() - 1; j < ring.size(); k = j++) {
                if (segmentsIntersect(a, b, ring.get(k), ring.get(j))) crossings++;
            }
        }
        return crossings;
    }

    public static double distanceToRing(@NotNull Vector2d point, @NotNull List<Vector2d> ring) {
        double min = Double.MAX_VALUE;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            min = Math.min(min, distanceToSegment(point, ring.get(j), ring.get(i)));
        }
        return min;
    }

    public static double angleBetween(double ax, double ay, double bx, double by) {
        final double la = Math.hypot(ax, ay);
        final double lb = Math.hypot(bx, by);
        if (la < EPSILON || lb < EPSILON) return 0;
        final double cos = Math.clamp((ax * bx + ay * by) / (la * lb), -1.0, 1.0);
        return Math.acos(cos);
    }

    private static boolean segmentsIntersect(@NotNull Vector2d a, @NotNull Vector2d b,
                                             @NotNull Vector2d c, @NotNull Vector2d d) {
        final double d1 = cross(c, d, a);
        final double d2 = cross(c, d, b);
        final double d3 = cross(a, b, c);
        final double d4 = cross(a, b, d);
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    private static double cross(@NotNull Vector2d from, @NotNull Vector2d to, @NotNull Vector2d point) {
        return (to.x - from.x) * (point.y - from.y) - (to.y - from.y) * (point.x - from.x);
    }

    private static double distanceToSegment(@NotNull Vector2d p, @NotNull Vector2d a, @NotNull Vector2d b) {
        final double abx = b.x - a.x;
        final double aby = b.y - a.y;
        final double lengthSquared = abx * abx + aby * aby;
        if (lengthSquared < EPSILON) return p.distance(a);
        final double t = Math.clamp(((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSquared, 0.0, 1.0);
        return p.distance(new Vector2d(a.x + t * abx, a.y + t * aby));
    }

    private static @NotNull Vector3d centroid3(@NotNull List<Vec3> points) {
        final var c = new Vector3d();
        for (final var p : points) c.add(p.x(), p.y(), p.z());
        return c.div(Math.max(1, points.size()));
    }

    private static @NotNull List<Vector2d> uniformCopies(@NotNull Vector2d point, int samples) {
        final var result = new ArrayList<Vector2d>(samples);
        for (int i = 0; i < samples; i++) result.add(new Vector2d(point));
        return result;
    }
}
