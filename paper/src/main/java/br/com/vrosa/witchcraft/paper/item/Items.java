package br.com.vrosa.witchcraft.paper.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Items {

    private Items() {}

    public static @NotNull ItemStack segment(int rgb) {
        return tinted(Keys.SEGMENT_MODEL, rgb);
    }

    public static @NotNull ItemStack selection(int tint) {
        return tinted(Keys.SELECTION_MODEL, tint);
    }

    private static @NotNull ItemStack tinted(@NotNull Key model, int rgb) {
        final var item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, model);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addColor(Color.fromRGB(rgb & 0xFFFFFF))
                .build());
        return item;
    }
}
