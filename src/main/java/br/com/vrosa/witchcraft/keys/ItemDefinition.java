package br.com.vrosa.witchcraft.keys;

import br.com.vrosa.witchcraft.WitchCraft;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import static br.com.vrosa.witchcraft.keys.WitchCraftKey.ITEM_TYPE;
import static br.com.vrosa.witchcraft.keys.WitchCraftKey.WITCHCRAFT_KEY;

public enum ItemDefinition {

    PENCIL("pencil", MiniMessage.miniMessage().deserialize("<yellow>Pencil</yellow>")),
    ERASER("eraser", MiniMessage.miniMessage().deserialize("<gray>Eraser</gray>")),
    PALLET("pallet", MiniMessage.miniMessage().deserialize("<aqua>Pallet</aqua>"));

    private final String id;
    private final Component name;

    ItemDefinition(String id, Component name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public ItemStack createStack() {
        final var item = new ItemStack(Material.CARROT_ON_A_STICK);
        final var meta = item.getItemMeta();

        if (meta == null) return item;
        meta.displayName(name);
        final var pdc = meta.getPersistentDataContainer();

        pdc.set(new NamespacedKey(WITCHCRAFT_KEY.getKey(), ITEM_TYPE.getKey()), PersistentDataType.STRING, this.getId());
        meta.setItemModel(new NamespacedKey(WITCHCRAFT_KEY.getKey(), this.getId()));
        item.setItemMeta(meta);

        return item;
    }

    public static @Nullable ItemDefinition getDefinitionById(String id) {
        for (final var item : values()) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }

    public static @Nullable ItemDefinition getType(ItemStack item) {
        if (item.getType() != Material.CARROT_ON_A_STICK) return null;

        final var meta = item.getItemMeta();
        if (meta == null) return null;

        final var pdc = meta.getPersistentDataContainer();
        if (!pdc.has(new NamespacedKey(WITCHCRAFT_KEY.getKey(), ITEM_TYPE.getKey()))) return null;

        final var type = pdc.get(new NamespacedKey(WITCHCRAFT_KEY.getKey(), ITEM_TYPE.getKey()), PersistentDataType.STRING);
        if (type == null) return null;

        return getDefinitionById(type);
    }

    public static boolean hasAnyType(ItemStack item) {
        if (item.getType() != Material.CARROT_ON_A_STICK) return false;

        final var meta = item.getItemMeta();
        if (meta == null) return false;

        final var pdc = meta.getPersistentDataContainer();
        return pdc.has(new NamespacedKey(WITCHCRAFT_KEY.getKey(), ITEM_TYPE.getKey()));
    }
}
