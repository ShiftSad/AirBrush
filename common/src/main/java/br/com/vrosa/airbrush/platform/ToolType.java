package br.com.vrosa.airbrush.platform;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ToolType {

    PENCIL("pencil", NamedTextColor.YELLOW),
    ERASER("eraser", NamedTextColor.GRAY),
    PALETTE("palette", TextColor.color(0x55FFFF));

    private final String id;
    private final TextColor color;

    ToolType(String id, TextColor color) {
        this.id = id;
        this.color = color;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull TextColor color() {
        return color;
    }

    public static @Nullable ToolType byId(@Nullable String id) {
        if (id == null) return null;
        for (final var tool : values()) {
            if (tool.id.equals(id)) return tool;
        }
        return null;
    }
}
