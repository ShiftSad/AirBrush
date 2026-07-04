package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.item.Keys;
import org.bukkit.persistence.PersistentDataType;
import br.com.vrosa.airbrush.platform.Cloth;
import br.com.vrosa.airbrush.platform.ResourcePackPrompt;
import br.com.vrosa.airbrush.platform.ToolType;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import br.com.vrosa.airbrush.platform.WorldRef;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record BukkitPlayer(@NotNull Player handle) implements WPlayer {

    public static @NotNull BukkitPlayer of(@NotNull Player player) {
        return new BukkitPlayer(player);
    }

    @Override
    public @NotNull UUID uuid() {
        return handle.getUniqueId();
    }

    @Override
    public @NotNull Locale locale() {
        return handle.locale();
    }

    @Override
    public @NotNull WorldRef world() {
        return new BukkitWorld(handle.getWorld());
    }

    @Override
    public @NotNull Vec3 eyePosition() {
        return BukkitWorld.toVec3(handle.getEyeLocation());
    }

    @Override
    public @NotNull Vector3f eyeDirection() {
        final var dir = handle.getEyeLocation().getDirection();
        return new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ());
    }

    @Override
    public float yaw() {
        return handle.getLocation().getYaw();
    }

    @Override
    public boolean sneaking() {
        return handle.isSneaking();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return handle.hasPermission(permission);
    }

    @Override
    public @Nullable ToolType heldTool() {
        return ItemFactory.toolOf(handle.getInventory().getItemInMainHand());
    }

    @Override
    public boolean holdingAnyTool() {
        return ItemFactory.hasAnyTool(handle.getInventory().getItemInMainHand());
    }

    @Override
    public void giveTool(@NotNull ToolType tool) {
        handle.getInventory().addItem(ItemFactory.create(tool, handle.locale()));
    }

    @Override
    public void damageHeldItem(int amount) {
        handle.damageItemStack(EquipmentSlot.HAND, amount);
    }

    @Override
    public void repairHeldItem(int amount) {
        final var item = handle.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof Damageable meta)) return;
        meta.setDamage(Math.max(0, meta.getDamage() - amount));
        item.setItemMeta(meta);
        handle.getInventory().setItemInMainHand(item);
    }

    @Override
    public int heldItemDurability() {
        final var item = handle.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof Damageable meta)) return 0;
        final int max = meta.hasMaxDamage() ? meta.getMaxDamage() : item.getType().getMaxDurability();
        return Math.max(0, max - meta.getDamage());
    }

    @Override
    public void consumeHeldItem() {
        final var inventory = handle.getInventory();
        final var item = inventory.getItemInMainHand();
        if (item.getAmount() <= 1) {
            inventory.setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
            inventory.setItemInMainHand(item);
        }
    }

    @Override
    public int heldToolTier() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        if (meta == null) return 1;
        final var tier = meta.getPersistentDataContainer().get(Keys.TIER, PersistentDataType.INTEGER);
        return tier == null ? 1 : tier;
    }

    @Override
    public double heldToolQuality() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        if (meta == null) return 1.0;
        final var quality = meta.getPersistentDataContainer().get(Keys.QUALITY, PersistentDataType.DOUBLE);
        return quality == null ? 1.0 : quality;
    }

    @Override
    public @Nullable Double heldToolRadius() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(Keys.RADIUS, PersistentDataType.DOUBLE);
    }

    @Override
    public void setHeldToolRadius(double radius) {
        editHeldItem(pdc -> pdc.set(Keys.RADIUS, PersistentDataType.DOUBLE, radius));
    }

    @Override
    public @Nullable String heldToolInk() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(Keys.INK, PersistentDataType.STRING);
    }

    @Override
    public @Nullable Integer heldToolInkColor() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(Keys.INK_COLOR, PersistentDataType.INTEGER);
    }

    @Override
    public void setHeldToolInk(@NotNull String inkId, int rgb, @NotNull Component inkLore) {
        final var item = handle.getInventory().getItemInMainHand();
        final var meta = item.getItemMeta();
        if (meta == null) return;

        final var pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.INK, PersistentDataType.STRING, inkId);
        pdc.set(Keys.INK_COLOR, PersistentDataType.INTEGER, rgb);

        // 1st line is the craft quality; the 2nd describes the loaded ink.
        final var lines = new java.util.ArrayList<Component>(2);
        final var existing = meta.lore();
        if (existing != null && !existing.isEmpty()) lines.add(existing.getFirst());
        lines.add(inkLore);
        meta.lore(lines);

        item.setItemMeta(meta);
        handle.getInventory().setItemInMainHand(item);
    }

    @Override
    public boolean heldClothWet() {
        final var meta = handle.getInventory().getItemInMainHand().getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(Keys.SOLVENT, PersistentDataType.STRING);
    }

    @Override
    public void setClothWet(@NotNull Component solventLore) {
        final var item = handle.getInventory().getItemInMainHand();
        item.editMeta(Damageable.class, meta -> {
            meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, Cloth.WET_MODEL));
            final var data = meta.getPersistentDataContainer();
            data.set(Keys.SOLVENT, PersistentDataType.STRING, "solvent");
            final var lines = new java.util.ArrayList<Component>(2);
            final var existing = meta.lore();
            if (existing != null && !existing.isEmpty()) lines.add(existing.getFirst());
            lines.add(solventLore);
            meta.lore(lines);
        });
    }

    @Override
    public void setClothDry() {
        final var item = handle.getInventory().getItemInMainHand();
        item.editMeta(meta -> {
            meta.setItemModel(new NamespacedKey(Keys.NAMESPACE, Cloth.ID));
            meta.getPersistentDataContainer().remove(Keys.SOLVENT);
            final var existing = meta.lore();
            if (existing != null && !existing.isEmpty()) meta.lore(List.of(existing.getFirst()));
        });
    }

    private void editHeldItem(@NotNull java.util.function.Consumer<org.bukkit.persistence.PersistentDataContainer> editor) {
        final var item = handle.getInventory().getItemInMainHand();
        final var meta = item.getItemMeta();
        if (meta == null) return;
        editor.accept(meta.getPersistentDataContainer());
        item.setItemMeta(meta);
        handle.getInventory().setItemInMainHand(item);
    }

    @Override
    public void actionBar(@NotNull Component message) {
        handle.sendActionBar(message);
    }

    @Override
    public void message(@NotNull Component message) {
        handle.sendMessage(message);
    }

    @Override
    public void playSound(@NotNull Key sound, float volume, float pitch) {
        handle.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch));
    }

    @Override
    public void sendResourcePack(@NotNull ResourcePackPrompt prompt) {
        handle.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(prompt.id(), URI.create(prompt.url()), prompt.hash()))
                .required(prompt.required())
                .build());
    }
}
