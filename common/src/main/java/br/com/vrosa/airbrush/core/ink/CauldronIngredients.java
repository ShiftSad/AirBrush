package br.com.vrosa.airbrush.core.ink;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Items that may go into a cauldron (ink/solvent recipes or dyes for mixing). */
public final class CauldronIngredients {

    private static final Set<String> RECIPE_ITEMS = recipeItems();

    private CauldronIngredients() {}

    public static boolean accepts(@Nullable String itemId) {
        if (itemId == null) return false;
        return RECIPE_ITEMS.contains(itemId) || Dyes.colorOf(itemId) != null;
    }

    private static Set<String> recipeItems() {
        final var items = new HashSet<String>();
        for (final var type : InkType.values()) items.addAll(type.ingredients().keySet());
        for (final var type : SolventType.values()) items.addAll(type.ingredients().keySet());
        return Set.copyOf(items);
    }
}
