package br.com.vrosa.witchcraft.minestom.item;

import br.com.vrosa.witchcraft.platform.ToolType;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MinestomItems {

    private MinestomItems() {}

    public static @NotNull ItemStack tool(@NotNull ToolType tool) {
        return ItemStack.builder(Material.CARROT_ON_A_STICK)
                .set(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + tool.id())
                .customName(tool.displayName())
                .set(Tags.ITEM_TYPE, tool.id())
                .build();
    }

    public static @NotNull ItemStack segment(int rgb) {
        return tinted(Tags.SEGMENT_MODEL, rgb);
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

    private static @NotNull ItemStack tinted(@NotNull String model, int rgb) {
        final var customModelData = new CustomModelData(
                List.of(), List.of(), List.of(), List.of(TextColor.color(rgb & 0xFFFFFF)));
        return ItemStack.builder(Material.PAPER)
                .set(DataComponents.ITEM_MODEL, model)
                .set(DataComponents.CUSTOM_MODEL_DATA, customModelData)
                .build();
    }
}
