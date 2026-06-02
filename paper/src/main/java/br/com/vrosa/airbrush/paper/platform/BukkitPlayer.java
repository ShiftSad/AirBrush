package br.com.vrosa.airbrush.paper.platform;

import br.com.vrosa.airbrush.paper.item.ItemFactory;
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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.net.URI;
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
