package br.com.vrosa.airbrush.core.ink;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Dyes {

    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("minecraft:white_dye", 0xF9FFFE),
            Map.entry("minecraft:light_gray_dye", 0x9D9D97),
            Map.entry("minecraft:gray_dye", 0x474F52),
            Map.entry("minecraft:black_dye", 0x1D1D21),
            Map.entry("minecraft:brown_dye", 0x835432),
            Map.entry("minecraft:red_dye", 0xB02E26),
            Map.entry("minecraft:orange_dye", 0xF9801D),
            Map.entry("minecraft:yellow_dye", 0xFED83D),
            Map.entry("minecraft:lime_dye", 0x80C71F),
            Map.entry("minecraft:green_dye", 0x5E7C16),
            Map.entry("minecraft:cyan_dye", 0x169C9C),
            Map.entry("minecraft:light_blue_dye", 0x3AB3DA),
            Map.entry("minecraft:blue_dye", 0x3C44AA),
            Map.entry("minecraft:purple_dye", 0x8932B8),
            Map.entry("minecraft:magenta_dye", 0xC74EBD),
            Map.entry("minecraft:pink_dye", 0xF38BAA));

    private Dyes() {}

    public static @Nullable Integer colorOf(@Nullable String itemId) {
        return itemId == null ? null : COLORS.get(itemId);
    }

    /** Mixes 50/50 per channel — each dye pulls the hue toward itself. */
    public static int mix(int base, int dye) {
        final int r = (((base >> 16) & 0xFF) + ((dye >> 16) & 0xFF)) / 2;
        final int g = (((base >> 8) & 0xFF) + ((dye >> 8) & 0xFF)) / 2;
        final int b = ((base & 0xFF) + (dye & 0xFF)) / 2;
        return (r << 16) | (g << 8) | b;
    }
}
