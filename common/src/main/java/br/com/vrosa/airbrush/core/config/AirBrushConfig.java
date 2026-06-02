package br.com.vrosa.airbrush.core.config;

import org.jetbrains.annotations.NotNull;

public final class AirBrushConfig {

    private static final ConfigSource DEFAULTS = new ConfigSource() {
        @Override
        public double getDouble(@NotNull String key, double fallback) {
            return fallback;
        }

        @Override
        public int getInt(@NotNull String key, int fallback) {
            return fallback;
        }

        @Override
        public boolean getBoolean(@NotNull String key, boolean fallback) {
            return fallback;
        }

        @Override
        public @NotNull String getString(@NotNull String key, @NotNull String fallback) {
            return fallback;
        }
    };

    private double maxRaycastLength;
    private float segmentDepth;
    private int smoothIterations;

    private double pencilDefaultRadius;
    private double pencilMinRadius;
    private double pencilMaxRadius;
    private double pencilRadiusStep;

    private double eraserDefaultRadius;
    private double eraserMinRadius;
    private double eraserMaxRadius;
    private double eraserRadiusStep;

    private boolean resourcePackEnabled;
    private String resourcePackIp;
    private int resourcePackPort;

    public AirBrushConfig() {
        load(DEFAULTS);
    }

    public void load(@NotNull ConfigSource source) {
        maxRaycastLength = source.getDouble("max-raycast-length", 10.0);
        segmentDepth = (float) source.getDouble("segment-depth", 0.03);
        smoothIterations = source.getInt("smooth-iterations", 2);

        pencilDefaultRadius = source.getDouble("pencil.default-radius", 0.05);
        pencilMinRadius = source.getDouble("pencil.min-radius", 0.01);
        pencilMaxRadius = source.getDouble("pencil.max-radius", 0.1);
        pencilRadiusStep = source.getDouble("pencil.radius-step", 0.01);

        eraserDefaultRadius = source.getDouble("eraser.default-radius", 0.5);
        eraserMinRadius = source.getDouble("eraser.min-radius", 0.125);
        eraserMaxRadius = source.getDouble("eraser.max-radius", 4.0);
        eraserRadiusStep = source.getDouble("eraser.radius-step", 0.125);

        resourcePackEnabled = source.getBoolean("resource-pack.enabled", true);
        resourcePackIp = source.getString("resource-pack.ip", "127.0.0.1");
        resourcePackPort = source.getInt("resource-pack.port", 8080);
    }

    public double maxRaycastLength() {
        return maxRaycastLength;
    }

    public float segmentDepth() {
        return segmentDepth;
    }

    public int smoothIterations() {
        return smoothIterations;
    }

    public double pencilDefaultRadius() {
        return pencilDefaultRadius;
    }

    public double pencilMinRadius() {
        return pencilMinRadius;
    }

    public double pencilMaxRadius() {
        return pencilMaxRadius;
    }

    public double pencilRadiusStep() {
        return pencilRadiusStep;
    }

    public double eraserDefaultRadius() {
        return eraserDefaultRadius;
    }

    public double eraserMinRadius() {
        return eraserMinRadius;
    }

    public double eraserMaxRadius() {
        return eraserMaxRadius;
    }

    public double eraserRadiusStep() {
        return eraserRadiusStep;
    }

    public boolean resourcePackEnabled() {
        return resourcePackEnabled;
    }

    public String resourcePackIp() {
        return resourcePackIp;
    }

    public int resourcePackPort() {
        return resourcePackPort;
    }
}
