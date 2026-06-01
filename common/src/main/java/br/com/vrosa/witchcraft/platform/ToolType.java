package br.com.vrosa.witchcraft.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ToolType {

    PENCIL("pencil", Component.text("Pencil", NamedTextColor.YELLOW)),
    ERASER("eraser", Component.text("Eraser", NamedTextColor.GRAY)),
    PALLET("pallet", Component.text("Pallet", TextColor.color(0x55FFFF)));

    private final String id;
    private final Component displayName;

    ToolType(String id, Component displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Component displayName() {
        return displayName;
    }

    public static @Nullable ToolType byId(@Nullable String id) {
        if (id == null) return null;
        for (final var tool : values()) {
            if (tool.id.equals(id)) return tool;
        }
        return null;
    }
}
