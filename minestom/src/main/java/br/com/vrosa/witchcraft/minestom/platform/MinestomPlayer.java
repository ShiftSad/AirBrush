package br.com.vrosa.witchcraft.minestom.platform;

import br.com.vrosa.witchcraft.minestom.item.MinestomItems;
import br.com.vrosa.witchcraft.platform.ToolType;
import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WPlayer;
import br.com.vrosa.witchcraft.platform.WorldRef;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    public @Nullable ToolType heldTool() {
        return MinestomItems.toolOf(handle.getItemInMainHand());
    }

    @Override
    public boolean holdingAnyTool() {
        return MinestomItems.hasAnyTool(handle.getItemInMainHand());
    }

    @Override
    public void giveTool(@NotNull ToolType tool) {
        handle.getInventory().addItemStack(MinestomItems.tool(tool));
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
}
