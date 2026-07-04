package br.com.vrosa.airbrush.platform;

import br.com.vrosa.airbrush.core.render.SegmentGeometry;
import org.jetbrains.annotations.NotNull;

public final class Cloth {

    public static final String ID = "cloth";
    public static final String WET_MODEL = "cloth_wet";

    private static final int MIN_CAPACITY = 100;
    private static final int MAX_CAPACITY = 300;

    private Cloth() {}

    public static int capacity(double quality) {
        return (int) Math.round(MIN_CAPACITY + (MAX_CAPACITY - MIN_CAPACITY) * Math.clamp(quality, 0.0, 1.0));
    }

    public static double eraseCost(@NotNull Transform transform, double blocksPerDurability) {
        return SegmentGeometry.volume(transform) / blocksPerDurability;
    }
}
