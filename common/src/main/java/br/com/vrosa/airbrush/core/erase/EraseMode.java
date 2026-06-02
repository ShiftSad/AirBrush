package br.com.vrosa.airbrush.core.erase;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public enum EraseMode {

    AREA(0x55FFFF),
    STROKE(0xFF5555);

    private final int tint;

    EraseMode(int tint) {
        this.tint = tint;
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
