package br.com.vrosa.airbrush.paper.item;

import br.com.vrosa.airbrush.core.glyph.craft.GlyphRecipes;
import br.com.vrosa.airbrush.core.glyph.craft.QualityGrade;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.ink.InkType;
import br.com.vrosa.airbrush.platform.AmethystDye;
import br.com.vrosa.airbrush.platform.CarriedInk;
import br.com.vrosa.airbrush.platform.Cloth;
import br.com.vrosa.airbrush.platform.Hammer;
import br.com.vrosa.airbrush.platform.Quill;
import br.com.vrosa.airbrush.platform.ToolType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class ItemFactory {

    private ItemFactory() {}

    public static @NotNull ItemStack create(@NotNull ToolType tool, @NotNull Locale locale) {
        if (tool == ToolType.MARKER) return amethystDye();
        if (tool == ToolType.QUILL) return quill(1, 1.0);
        if (tool == ToolType.CLOTH) return cloth(1.0);

        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        final var meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(Messages.toolName(locale, tool), tool.color()));
        meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, tool.id());
        meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, tool.id()));
        item.setItemMeta(meta);
        return item;
    }

    public static @NotNull ItemStack hammer() {
        final var item = new ItemStack(Material.IRON_PICKAXE);
        item.editMeta(Damageable.class, meta -> {
            meta.itemName(Component.translatable(Hammer.NAME_KEY));
            meta.setItemModel(Keys.HAMMER_MODEL);
            meta.setMaxDamage(Hammer.DURABILITY);

            final var tool = meta.getTool();
            tool.setDamagePerBlock(1);
            tool.addRule(Tag.INCORRECT_FOR_IRON_TOOL, null, false);
            tool.addRule(Tag.MINEABLE_PICKAXE, Hammer.MINING_SPEED, true);
            meta.setTool(tool);

            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    NamespacedKey.minecraft("base_attack_damage"), Hammer.ATTACK_DAMAGE_MODIFIER,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                    NamespacedKey.minecraft("base_attack_speed"), Hammer.ATTACK_SPEED_MODIFIER,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        });
        return item;
    }

    public static @Nullable ItemStack craftResult(@NotNull String recipeId, double quality, @Nullable CarriedInk ink) {
        return switch (recipeId) {
            case GlyphRecipes.QUILL_ID -> quill(1, quality, ink);
            case GlyphRecipes.QUILL_GOLD_ID -> quill(2, quality, ink);
            case GlyphRecipes.QUILL_DIAMOND_ID -> quill(3, quality, ink);
            case GlyphRecipes.QUILL_NETHERITE_ID -> quill(4, quality, ink);
            case GlyphRecipes.CLOTH_ID -> cloth(quality);
            default -> null;
        };
    }

    public static @NotNull ItemStack quill(int tier, double quality) {
        return quill(tier, quality, null);
    }

    public static @NotNull ItemStack quill(int tier, double quality, @Nullable CarriedInk carried) {
        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        item.editMeta(Damageable.class, meta -> {
            final var id = Quill.idOf(tier);
            final int capacity = Quill.capacity(tier, quality);
            meta.itemName(Component.translatable("item." + Keys.NAMESPACE + "." + id,
                    QualityGrade.of(quality).color()));
            meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, id));
            meta.setMaxDamage(capacity);
            final var data = meta.getPersistentDataContainer();
            data.set(Keys.ITEM_TYPE, PersistentDataType.STRING, id);
            data.set(Keys.TIER, PersistentDataType.INTEGER, tier);
            data.set(Keys.QUALITY, PersistentDataType.DOUBLE, quality);

            final var lore = new java.util.ArrayList<Component>(2);
            lore.add(qualityLore(quality));

            // With no inherited ink the quill starts empty: 1 durability left until the cauldron.
            int charge = 1;
            final var ink = carried == null ? null : InkType.byId(carried.inkId());
            if (ink != null) {
                charge = Math.clamp(carried.charge(), 1, capacity);
                lore.add(ink.loreLine(carried.rgb()));
                data.set(Keys.INK, PersistentDataType.STRING, ink.id());
                data.set(Keys.INK_COLOR, PersistentDataType.INTEGER, carried.rgb());
            }
            meta.setDamage(capacity - charge);
            meta.lore(lore);
        });
        return item;
    }

    public static @NotNull ItemStack cloth(double quality) {
        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        item.editMeta(Damageable.class, meta -> {
            final int capacity = Cloth.capacity(quality);
            meta.itemName(Component.translatable("item." + Keys.NAMESPACE + "." + Cloth.ID,
                    QualityGrade.of(quality).color()));
            meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, Cloth.ID));
            meta.setMaxDamage(capacity);
            meta.setDamage(capacity - 1);
            final var data = meta.getPersistentDataContainer();
            data.set(Keys.ITEM_TYPE, PersistentDataType.STRING, Cloth.ID);
            data.set(Keys.QUALITY, PersistentDataType.DOUBLE, quality);
            meta.lore(List.of(qualityLore(quality)));
        });
        return item;
    }

    private static @NotNull Component qualityLore(double quality) {
        return Component.translatable(QualityGrade.of(quality).translationKey(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static boolean isHammer(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.IRON_PICKAXE) return false;
        final var meta = item.getItemMeta();
        return meta != null && meta.hasItemModel() && Keys.HAMMER_MODEL.equals(meta.getItemModel());
    }

    public static @NotNull ItemStack amethystDye() {
        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        item.editMeta(Damageable.class, meta -> {
            meta.itemName(Component.translatable(AmethystDye.NAME_KEY));
            meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, AmethystDye.ID));
            meta.setMaxDamage(AmethystDye.DURABILITY);
            meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, AmethystDye.ID);
        });
        return item;
    }

    public static @NotNull ShapedRecipe hammerRecipe() {
        final var recipe = new ShapedRecipe(Keys.HAMMER_RECIPE, hammer());
        recipe.shape("BIB", " S ", " S ");
        recipe.setIngredient('B', Material.IRON_BLOCK);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        return recipe;
    }

    public static @Nullable ToolType toolOf(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.CARROT_ON_A_STICK) return null;

        final var meta = item.getItemMeta();
        if (meta == null) return null;

        final var id = meta.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING);
        return ToolType.byId(id);
    }

    public static boolean hasAnyTool(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.CARROT_ON_A_STICK) return false;

        final var meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(Keys.ITEM_TYPE, PersistentDataType.STRING);
    }
}
