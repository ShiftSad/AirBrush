package br.com.vrosa.airbrush.minestom.item;

import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.platform.AmethystDye;
import br.com.vrosa.airbrush.platform.Hammer;
import br.com.vrosa.airbrush.platform.ToolType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
