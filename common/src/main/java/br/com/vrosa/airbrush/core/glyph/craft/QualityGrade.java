package br.com.vrosa.airbrush.core.glyph.craft;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public enum QualityGrade {

    CRUDE("crude", NamedTextColor.GRAY),
    DECENT("decent", NamedTextColor.YELLOW),
    GOOD("good", NamedTextColor.GREEN),
    PERFECT("perfect", NamedTextColor.LIGHT_PURPLE);

    private final String key;
    private final TextColor color;

    QualityGrade(String key, TextColor color) {
        this.key = key;
        this.color = color;
    }

    public @NotNull String translationKey() {
        return "airbrush.quality." + key;
    }

    public @NotNull TextColor color() {
        return color;
    }

    public static @NotNull QualityGrade of(double quality) {
        if (quality < 0.4) return CRUDE;
        if (quality < 0.7) return DECENT;
        if (quality < 0.9) return GOOD;
        return PERFECT;
    }
}
