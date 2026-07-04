package br.com.vrosa.airbrush.minestom.item;

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
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlotGroup;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.item.component.Tool;
import net.minestom.server.registry.RegistryTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MinestomItems {

    private static final String HAMMER_MODEL = Tags.NAMESPACE + ":" + Hammer.ID;

    private MinestomItems() {}

    public static @NotNull ItemStack tool(@NotNull ToolType tool, @NotNull Locale locale) {
        if (tool == ToolType.MARKER) return amethystDye();
        if (tool == ToolType.QUILL) return quill(1, 1.0);
        if (tool == ToolType.CLOTH) return cloth(1.0);

        return ItemStack.builder(Material.CARROT_ON_A_STICK)
                .set(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + tool.id())
                .customName(Component.text(Messages.toolName(locale, tool), tool.color()))
                .set(Tags.ITEM_TYPE, tool.id())
                .build();
    }

    public static @NotNull ItemStack hammer() {
        final var tool = new Tool(List.of(
                new Tool.Rule(blockTag("minecraft:incorrect_for_iron_tool"), null, false),
                new Tool.Rule(blockTag("minecraft:mineable/pickaxe"), Hammer.MINING_SPEED, true)),
                Tool.DEFAULT_MINING_SPEED, Tool.DEFAULT_DAMAGE_PER_BLOCK, true);

        final var attributes = new AttributeList(List.of(
                new AttributeList.Modifier(Attribute.ATTACK_DAMAGE,
                        new AttributeModifier("minecraft:base_attack_damage", Hammer.ATTACK_DAMAGE_MODIFIER, AttributeOperation.ADD_VALUE),
                        EquipmentSlotGroup.MAIN_HAND),
                new AttributeList.Modifier(Attribute.ATTACK_SPEED,
                        new AttributeModifier("minecraft:base_attack_speed", Hammer.ATTACK_SPEED_MODIFIER, AttributeOperation.ADD_VALUE),
                        EquipmentSlotGroup.MAIN_HAND)));

        return ItemStack.builder(Material.IRON_PICKAXE)
                .set(DataComponents.ITEM_MODEL, HAMMER_MODEL)
                .set(DataComponents.ITEM_NAME, Component.translatable(Hammer.NAME_KEY))
                .set(DataComponents.MAX_DAMAGE, Hammer.DURABILITY)
                .set(DataComponents.TOOL, tool)
                .set(DataComponents.ATTRIBUTE_MODIFIERS, attributes)
                .build();
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
        final var id = Quill.idOf(tier);
        final int capacity = Quill.capacity(tier, quality);

        final var lore = new java.util.ArrayList<Component>(2);
        lore.add(qualityLore(quality));

        // With no inherited ink the quill starts empty: 1 durability left until the cauldron.
        int charge = 1;
        final var ink = carried == null ? null : InkType.byId(carried.inkId());
        if (ink != null) {
            charge = Math.clamp(carried.charge(), 1, capacity);
            lore.add(ink.loreLine(carried.rgb()));
        }

        var builder = ItemStack.builder(Material.CARROT_ON_A_STICK)
                .set(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + id)
                .set(DataComponents.ITEM_NAME, Component.translatable(
                        "item." + Tags.NAMESPACE + "." + id, QualityGrade.of(quality).color()))
                .set(DataComponents.LORE, (List<Component>) lore)
                .set(DataComponents.MAX_DAMAGE, capacity)
                .set(DataComponents.DAMAGE, capacity - charge)
                .set(Tags.ITEM_TYPE, id)
                .set(Tags.TIER, tier)
                .set(Tags.QUALITY, quality);
        if (ink != null) {
            builder = builder.set(Tags.INK, ink.id()).set(Tags.INK_COLOR, carried.rgb());
        }
        return builder.build();
    }

    public static @NotNull ItemStack cloth(double quality) {
        final int capacity = Cloth.capacity(quality);
        return ItemStack.builder(Material.CARROT_ON_A_STICK)
                .set(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + Cloth.ID)
                .set(DataComponents.ITEM_NAME, Component.translatable(
                        "item." + Tags.NAMESPACE + "." + Cloth.ID, QualityGrade.of(quality).color()))
                .set(DataComponents.LORE, List.of(qualityLore(quality)))
                .set(DataComponents.MAX_DAMAGE, capacity)
                .set(DataComponents.DAMAGE, capacity - 1)
                .set(Tags.ITEM_TYPE, Cloth.ID)
                .set(Tags.QUALITY, quality)
                .build();
    }

    private static @NotNull Component qualityLore(double quality) {
        return Component.translatable(QualityGrade.of(quality).translationKey(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static boolean isHammer(@Nullable ItemStack item) {
        return item != null && item.material() == Material.IRON_PICKAXE
                && HAMMER_MODEL.equals(item.get(DataComponents.ITEM_MODEL));
    }

    public static @NotNull ItemStack amethystDye() {
        return ItemStack.builder(Material.CARROT_ON_A_STICK)
                .set(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + AmethystDye.ID)
                .set(DataComponents.ITEM_NAME, Component.translatable(AmethystDye.NAME_KEY))
                .set(DataComponents.MAX_DAMAGE, AmethystDye.DURABILITY)
                .set(Tags.ITEM_TYPE, AmethystDye.ID)
                .build();
    }

    public static @NotNull ItemStack segment(int rgb, boolean bright) {
        final var customModelData = new CustomModelData(
                List.of(), List.of(), bright ? List.of("active") : List.of(), List.of(TextColor.color(rgb & 0xFFFFFF)));
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, Tags.SEGMENT_MODEL)
                .set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
                .build();
    }

    public static @NotNull ItemStack selection(int tint) {
        return tinted(Tags.SELECTION_MODEL, tint);
    }

    public static @Nullable ToolType toolOf(@Nullable ItemStack item) {
        if (item == null || item.isAir()) return null;
        return ToolType.byId(item.getTag(Tags.ITEM_TYPE));
    }

    public static boolean hasAnyTool(@Nullable ItemStack item) {
        return toolOf(item) != null;
    }

    private static @NotNull RegistryTag<Block> blockTag(@NotNull String name) {
        return Objects.requireNonNull(Block.staticRegistry().getTag(Key.key(name)), name);
    }

    private static @NotNull ItemStack tinted(@NotNull String model, int rgb) {
        final var customModelData = new CustomModelData(
                List.of(), List.of(), List.of(), List.of(TextColor.color(rgb & 0xFFFFFF)));
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, model)
                .set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
                .build();
    }
}
