package br.com.vrosa.airbrush.core.glyph.craft;

import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GlyphRecipes {

    public static final String QUILL_ID = "quill";
    public static final String QUILL_GOLD_ID = "quill_gold";
    public static final String QUILL_DIAMOND_ID = "quill_diamond";
    public static final String QUILL_NETHERITE_ID = "quill_netherite";
    public static final String CLOTH_ID = "cloth";

    private static final List<GlyphRecipe> ALL = List.of(
            // Quills: each tier asks for a slightly more complex glyph than the previous one,
            // a block of the metal and the quill of the tier below.
            new GlyphRecipe(QUILL_ID, BaseShape.TRIANGLE, Set.of(),
                    Map.of("minecraft:feather", 1, "minecraft:charcoal", 1)),
            new GlyphRecipe(QUILL_GOLD_ID, BaseShape.TRIANGLE, Set.of(Modifier.FOCUS_DOT),
                    Map.of("airbrush:quill", 1, "minecraft:gold_block", 1)),
            new GlyphRecipe(QUILL_DIAMOND_ID, BaseShape.TRIANGLE, Set.of(Modifier.FOCUS_DOT, Modifier.DIVISION_LINE),
                    Map.of("airbrush:quill_gold", 1, "minecraft:diamond_block", 1)),
            new GlyphRecipe(QUILL_NETHERITE_ID, BaseShape.TRIANGLE,
                    Set.of(Modifier.FOCUS_DOT, Modifier.DIVISION_LINE, Modifier.RAYS),
                    Map.of("airbrush:quill_diamond", 1, "minecraft:netherite_block", 1)),
            new GlyphRecipe(CLOTH_ID, BaseShape.SQUARE, Set.of(),
                    Map.of("minecraft:white_wool", 1)));

    private GlyphRecipes() {}

    public static @NotNull List<GlyphRecipe> all() {
        return ALL;
    }

    public static @NotNull Optional<GlyphRecipe> match(@NotNull BaseShape base, @NotNull Set<Modifier> modifiers,
                                                       @NotNull Map<String, Integer> available) {
        for (final var recipe : ALL) {
            if (recipe.base() == base && recipe.modifiers().equals(modifiers) && hasAll(available, recipe)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static boolean hasAll(@NotNull Map<String, Integer> available, @NotNull GlyphRecipe recipe) {
        for (final var entry : recipe.ingredients().entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }
}
