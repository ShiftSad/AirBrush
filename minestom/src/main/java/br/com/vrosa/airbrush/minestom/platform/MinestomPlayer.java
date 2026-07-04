package br.com.vrosa.airbrush.minestom.platform;

import br.com.vrosa.airbrush.minestom.item.MinestomItems;
import br.com.vrosa.airbrush.minestom.item.Tags;
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
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

public record MinestomPlayer(@NotNull Player handle) implements WPlayer {

    public static @NotNull MinestomPlayer of(@NotNull Player player) {
        return new MinestomPlayer(player);
    }

    @Override
    public @NotNull UUID uuid() {
        return handle.getUuid();
    }

    @Override
    public @NotNull Locale locale() {
        final var locale = handle.getLocale();
        return locale != null ? locale : Locale.US;
    }

    @Override
    public @NotNull WorldRef world() {
        return new MinestomWorld(handle.getInstance());
    }

    @Override
    public @NotNull Vec3 eyePosition() {
        final var pos = handle.getPosition();
        return new Vec3(pos.x(), pos.y() + handle.getEyeHeight(), pos.z());
    }

    @Override
    public @NotNull Vector3f eyeDirection() {
        final var dir = handle.getPosition().direction();
        return new Vector3f((float) dir.x(), (float) dir.y(), (float) dir.z());
    }

    @Override
    public float yaw() {
        return handle.getPosition().yaw();
    }

    @Override
    public boolean sneaking() {
        return handle.isSneaking();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        // The sandbox has no permission plugin; operator level covers everything.
        return handle.getPermissionLevel() >= 4;
    }

    @Override
    public @Nullable ToolType heldTool() {
        return MinestomItems.toolOf(handle.getItemInMainHand());
    }

    @Override
    public boolean holdingAnyTool() {
        return MinestomItems.hasAnyTool(handle.getItemInMainHand());
    }

    @Override
    public void giveTool(@NotNull ToolType tool) {
        handle.getInventory().addItemStack(MinestomItems.tool(tool, locale()));
    }

    @Override
    public void damageHeldItem(int amount) {
        final var item = handle.getItemInMainHand();
        final int max = item.get(DataComponents.MAX_DAMAGE, 0);
        if (max <= 0) return;

        final int damage = item.get(DataComponents.DAMAGE, 0) + amount;
        if (damage >= max) {
            handle.setItemInMainHand(ItemStack.AIR);
            handle.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 1f, 1f));
        } else {
            handle.setItemInMainHand(item.with(DataComponents.DAMAGE, damage));
        }
    }

    @Override
    public void repairHeldItem(int amount) {
        final var item = handle.getItemInMainHand();
        if (item.get(DataComponents.MAX_DAMAGE, 0) <= 0) return;

        final int damage = Math.max(0, item.get(DataComponents.DAMAGE, 0) - amount);
        handle.setItemInMainHand(item.with(DataComponents.DAMAGE, damage));
    }

    @Override
    public int heldItemDurability() {
        final var item = handle.getItemInMainHand();
        final int max = item.get(DataComponents.MAX_DAMAGE, 0);
        if (max <= 0) return 0;
        return Math.max(0, max - item.get(DataComponents.DAMAGE, 0));
    }

    @Override
    public void consumeHeldItem() {
        final var item = handle.getItemInMainHand();
        handle.setItemInMainHand(item.amount() <= 1 ? ItemStack.AIR : item.withAmount(item.amount() - 1));
    }

    @Override
    public int heldToolTier() {
        final var tier = handle.getItemInMainHand().getTag(Tags.TIER);
        return tier == null ? 1 : tier;
    }

    @Override
    public double heldToolQuality() {
        final var quality = handle.getItemInMainHand().getTag(Tags.QUALITY);
        return quality == null ? 1.0 : quality;
    }

    @Override
    public @Nullable Double heldToolRadius() {
        return handle.getItemInMainHand().getTag(Tags.RADIUS);
    }

    @Override
    public void setHeldToolRadius(double radius) {
        final var item = handle.getItemInMainHand();
        if (item.isAir()) return;
        handle.setItemInMainHand(item.withTag(Tags.RADIUS, radius));
    }

    @Override
    public @Nullable String heldToolInk() {
        return handle.getItemInMainHand().getTag(Tags.INK);
    }

    @Override
    public @Nullable Integer heldToolInkColor() {
        return handle.getItemInMainHand().getTag(Tags.INK_COLOR);
    }

    @Override
    public void setHeldToolInk(@NotNull String inkId, int rgb, @NotNull Component inkLore) {
        final var item = handle.getItemInMainHand();
        if (item.isAir()) return;

        // 1st line is the craft quality; the 2nd describes the loaded ink.
        final var existing = item.get(DataComponents.LORE);
        final var lines = new java.util.ArrayList<Component>(2);
        if (existing != null && !existing.isEmpty()) lines.add(existing.getFirst());
        lines.add(inkLore);

        handle.setItemInMainHand(item.withTag(Tags.INK, inkId).withTag(Tags.INK_COLOR, rgb)
                .with(DataComponents.LORE, (java.util.List<Component>) lines));
    }

    @Override
    public boolean heldClothWet() {
        return handle.getItemInMainHand().getTag(Tags.SOLVENT) != null;
    }

    @Override
    public void setClothWet(@NotNull Component solventLore) {
        final var item = handle.getItemInMainHand();
        if (item.isAir()) return;

        final var existing = item.get(DataComponents.LORE);
        final var lines = new java.util.ArrayList<Component>(2);
        if (existing != null && !existing.isEmpty()) lines.add(existing.getFirst());
        lines.add(solventLore);

        handle.setItemInMainHand(item
                .with(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + Cloth.WET_MODEL)
                .withTag(Tags.SOLVENT, "solvent")
                .with(DataComponents.LORE, (java.util.List<Component>) lines));
    }

    @Override
    public void setClothDry() {
        final var item = handle.getItemInMainHand();
        if (item.isAir()) return;

        final var existing = item.get(DataComponents.LORE);
        final var lines = existing != null && !existing.isEmpty()
                ? java.util.List.of(existing.getFirst())
                : java.util.List.<Component>of();

        handle.setItemInMainHand(item
                .with(DataComponents.ITEM_MODEL, Tags.NAMESPACE + ":" + Cloth.ID)
                .withTag(Tags.SOLVENT, null)
                .with(DataComponents.LORE, (java.util.List<Component>) lines));
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
