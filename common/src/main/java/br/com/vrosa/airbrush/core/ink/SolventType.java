package br.com.vrosa.airbrush.core.ink;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public enum SolventType {

    COMMON("solvent", 0x9BB89A,
            Map.of("minecraft:slime_ball", 1, "minecraft:fermented_spider_eye", 1));

    private final String id;
    private final int waterRgb;
    private final Map<String, Integer> ingredients;

    SolventType(String id, int waterRgb, Map<String, Integer> ingredients) {
        this.id = id;
        this.waterRgb = waterRgb;
        this.ingredients = ingredients;
    }

    public @NotNull String id() {
        return id;
    }

    public int waterRgb() {
        return waterRgb;
    }

    public @NotNull Map<String, Integer> ingredients() {
        return ingredients;
    }

    public @NotNull Component loreLine() {
        return Component.text("■ ", TextColor.color(waterRgb))
                .append(Component.translatable("airbrush.solvent." + id, NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static @Nullable SolventType byId(@Nullable String id) {
        if (id == null) return null;
        for (final var type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    public static @NotNull Optional<SolventType> match(@NotNull Map<String, Integer> available) {
        for (final var type : values()) {
            if (hasAll(available, type.ingredients)) return Optional.of(type);
        }
        return Optional.empty();
    }

    private static boolean hasAll(@NotNull Map<String, Integer> available, @NotNull Map<String, Integer> ingredients) {
        for (final var entry : ingredients.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }
}
