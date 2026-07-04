package br.com.vrosa.airbrush.core.ink;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum InkType {

    // Recipes with more ingredients come first: match() returns the first satisfied recipe,
    // so a recipe that is a subset of another must not precede it
    // (INDELIBLE_LUMINOUS ⊃ INDELIBLE, LUMINOUS ⊃ AMETHYST).
    INDELIBLE_LUMINOUS("indelible_luminous", 0x3B7BFF, true, true,
            Map.of("minecraft:echo_shard", 1, "minecraft:diamond", 1,
                    "minecraft:amethyst_block", 1, "minecraft:glow_ink_sac", 1)),
    INDELIBLE("indelible", 0x0E3B3F, false, true,
            Map.of("minecraft:echo_shard", 1, "minecraft:diamond", 1, "minecraft:amethyst_block", 1)),
    LUMINOUS("luminous", 0x4DE3D1, true, false,
            Map.of("minecraft:glow_ink_sac", 1, "minecraft:amethyst_shard", 1)),
    AMETHYST("amethyst", 0xB784E0, false, false,
            Map.of("minecraft:amethyst_shard", 1)),
    COMMON("common", 0x1B2A4A, false, false,
            Map.of("minecraft:ink_sac", 1));

    private final String id;
    private final int waterRgb;
    private final boolean bright;
    private final boolean permanent;
    private final Map<String, Integer> ingredients;

    InkType(String id, int waterRgb, boolean bright, boolean permanent, Map<String, Integer> ingredients) {
        this.id = id;
        this.waterRgb = waterRgb;
        this.bright = bright;
        this.permanent = permanent;
        this.ingredients = ingredients;
    }

    public @NotNull String id() {
        return id;
    }

    public int waterRgb() {
        return waterRgb;
    }

    /** Strokes of this ink glow in the dark. */
    public boolean bright() {
        return bright;
    }

    public boolean permanent() {
        return permanent;
    }

    public @NotNull Map<String, Integer> ingredients() {
        return ingredients;
    }

    /** Ink lore line: a swatch in the color, the name (translated client-side) and the hex. */
    public @NotNull Component loreLine(int color) {
        return Component.text("■ ", TextColor.color(color))
                .append(Component.translatable("airbrush.ink." + id, NamedTextColor.GRAY))
                .append(Component.text(String.format(Locale.ROOT, " #%06X", color), NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false);
    }

    public static @Nullable InkType byId(@Nullable String id) {
        if (id == null) return null;
        for (final var type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    public static @NotNull Optional<InkType> match(@NotNull Map<String, Integer> available) {
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
