package br.com.vrosa.airbrush.core.render;

import br.com.vrosa.airbrush.platform.Transform;
import org.jetbrains.annotations.NotNull;

public final class SegmentGeometry {

    private static final double MIN_VOLUME = 1.0e-6;

    private SegmentGeometry() {}

    /** Approximate segment volume (width × length) in block units. */
    public static double volume(@NotNull Transform transform) {
        final var scale = transform.scale();
        return Math.max(MIN_VOLUME, scale.x * scale.z);
    }
}
