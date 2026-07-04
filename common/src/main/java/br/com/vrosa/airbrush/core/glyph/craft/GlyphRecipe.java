package br.com.vrosa.airbrush.core.glyph.craft;

import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public record GlyphRecipe(@NotNull String id, @NotNull BaseShape base, @NotNull Set<Modifier> modifiers,
                          @NotNull Map<String, Integer> ingredients) {

    public GlyphRecipe {
        modifiers = Set.copyOf(modifiers);
        ingredients = Map.copyOf(ingredients);
    }
}
