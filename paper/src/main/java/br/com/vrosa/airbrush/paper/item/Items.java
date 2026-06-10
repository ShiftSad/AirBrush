package br.com.vrosa.airbrush.paper.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Items {

    private Items() {}

    public static @NotNull ItemStack segment(int rgb, boolean bright) {
        final var item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Keys.SEGMENT_MODEL);
        final var data = CustomModelData.customModelData().addColor(Color.fromRGB(rgb & 0xFFFFFF));
        if (bright) data.addString("active");
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, data.build());
        return item;
    }

    public static @NotNull ItemStack selection(int tint) {
        final var item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, Keys.SELECTION_MODEL);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addColor(Color.fromRGB(tint & 0xFFFFFF))
                .build());
        return item;
    }
}
