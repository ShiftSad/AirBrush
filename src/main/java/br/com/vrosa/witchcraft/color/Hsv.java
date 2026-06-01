package br.com.vrosa.witchcraft.color;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

public final class Hsv {

    private Hsv() {}

    public static @NotNull Color toColor(double h, double s, double v) {
        h = ((h % 360) + 360) % 360;
        final double c = v * s;
        final double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        final double m = v - c;

        final double r;
        final double g;
        final double b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        return Color.fromRGB(channel(r + m), channel(g + m), channel(b + m));
    }

    public static double[] toHsv(@NotNull Color color) {
        final double r = color.getRed() / 255.0;
        final double g = color.getGreen() / 255.0;
        final double b = color.getBlue() / 255.0;

        final double max = Math.max(r, Math.max(g, b));
        final double min = Math.min(r, Math.min(g, b));
        final double delta = max - min;

        double hue = 0;
        if (delta != 0) {
            if (max == r) hue = 60 * (((g - b) / delta) % 6);
            else if (max == g) hue = 60 * ((b - r) / delta + 2);
            else hue = 60 * ((r - g) / delta + 4);
            hue = ((hue % 360) + 360) % 360;
        }

        final double s = max == 0 ? 0 : delta / max;
        return new double[]{hue, s, max};
    }

    private static int channel(double value) {
        return Math.clamp((int) Math.round(value * 255), 0, 255);
    }
}
