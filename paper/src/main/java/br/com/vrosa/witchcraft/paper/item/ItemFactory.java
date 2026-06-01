package br.com.vrosa.witchcraft.paper.item;

import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.platform.ToolType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ItemFactory {

    private ItemFactory() {}

    public static @NotNull ItemStack create(@NotNull ToolType tool, @NotNull Locale locale) {
        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        final var meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(Messages.toolName(locale, tool), tool.color()));
        meta.getPersistentDataContainer().set(Keys.ITEM_TYPE, PersistentDataType.STRING, tool.id());
        meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, tool.id()));
        item.setItemMeta(meta);
        return item;
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
