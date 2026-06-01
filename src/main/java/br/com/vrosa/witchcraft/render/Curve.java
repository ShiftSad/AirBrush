package br.com.vrosa.witchcraft.render;

import br.com.vrosa.witchcraft.raycast.RayHit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class Curve {

    private Curve() {}

    public static @NotNull List<RayHit> chaikin(@NotNull List<RayHit> in, int iterations) {
        var pts = in;
        for (int it = 0; it < iterations; it++) {
            final int n = pts.size();
            if (n < 3) return pts;

            final var out = new ArrayList<RayHit>();
            out.add(pts.getFirst().copy());
            for (int i = 0; i < n - 1; i++) {
                out.add(lerp(pts.get(i), pts.get(i + 1), 0.25));
                out.add(lerp(pts.get(i), pts.get(i + 1), 0.75));
            }
            out.add(pts.get(n - 1).copy());
            pts = out;
        }
        return pts;
    }

    private static @NotNull RayHit lerp(@NotNull RayHit a, @NotNull RayHit b, double t) {
        final var pa = a.position();
        final var pb = b.position();
        final var position = new Location(pa.getWorld(),
                pa.getX() + (pb.getX() - pa.getX()) * t,
                pa.getY() + (pb.getY() - pa.getY()) * t,
                pa.getZ() + (pb.getZ() - pa.getZ()) * t);

        final var normal = new Vector3f(a.normal()).lerp(b.normal(), (float) t);
        if (normal.lengthSquared() > 1.0e-8f) normal.normalize();
        return new RayHit(position, normal);
    }
}
