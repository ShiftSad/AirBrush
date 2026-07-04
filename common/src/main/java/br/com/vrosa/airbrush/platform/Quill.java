package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Quill {

    public static final String ID = "quill";
    public static final int TIERS = 4;

    private static final String[] IDS = {"quill", "quill_gold", "quill_diamond", "quill_netherite"};

    private static final int MIN_CAPACITY = 100;
    private static final int MAX_CAPACITY = 900;
    private static final double MAX_INK_EFFICIENCY = 3.0;
    private static final double MAX_RANGE_MULTIPLIER = 2.0;
    private static final double MIN_FINENESS = 0.25;

    private Quill() {}

    public static @NotNull String idOf(int tier) {
        return IDS[Math.clamp(tier, 1, TIERS) - 1];
    }

    public static boolean isQuillId(@Nullable String id) {
        if (id == null) return false;
        for (final var candidate : IDS) {
            if (candidate.equals(id)) return true;
        }
        return false;
    }

    public static int tierOf(@Nullable String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) return i + 1;
        }
        return 1;
    }

    public static int capacity(int tier, double quality) {
        return (int) Math.round(lerp(MIN_CAPACITY, MAX_CAPACITY, progress(tier, quality)));
    }

    public static double inkEfficiency(int tier, double quality) {
        return lerp(1.0, MAX_INK_EFFICIENCY, progress(tier, quality));
    }

    public static double rangeMultiplier(int tier, double quality) {
        return lerp(1.0, MAX_RANGE_MULTIPLIER, progress(tier, quality));
    }

    public static double finenessMultiplier(int tier, double quality) {
        return lerp(1.0, MIN_FINENESS, progress(tier, quality));
    }

    private static double progress(int tier, double quality) {
        return ((Math.clamp(tier, 1, TIERS) - 1) + Math.clamp(quality, 0.0, 1.0)) / TIERS;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
