package br.com.vrosa.airbrush.core.render;

import br.com.vrosa.airbrush.platform.Pose;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class Curve {

    private Curve() {}

    public static @NotNull List<Pose> chaikin(@NotNull List<Pose> in, int iterations) {
        var pts = in;
        for (int it = 0; it < iterations; it++) {
            final int n = pts.size();
            if (n < 3) return pts;

            final var out = new ArrayList<Pose>();
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

    private static @NotNull Pose lerp(@NotNull Pose a, @NotNull Pose b, double t) {
        final var position = a.position().lerp(b.position(), t);
        final var normal = new Vector3f(a.normal()).lerp(b.normal(), (float) t);
        if (normal.lengthSquared() > 1.0e-8f) normal.normalize();
        return new Pose(a.world(), position, normal);
    }
}
