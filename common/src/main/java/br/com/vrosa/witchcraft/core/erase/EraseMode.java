package br.com.vrosa.witchcraft.core.erase;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public enum EraseMode {

    AREA("Área", 0x55FFFF),
    STROKE("Traço inteiro", 0xFF5555);

    private final String label;
    private final int tint;

    EraseMode(String label, int tint) {
        this.label = label;
        this.tint = tint;
    }

    public @NotNull String label() {
        return label;
    }

    public int tint() {
        return tint;
    }

    public @NotNull TextColor color() {
        return TextColor.color(tint);
    }

    public @NotNull EraseMode next() {
        final var values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
