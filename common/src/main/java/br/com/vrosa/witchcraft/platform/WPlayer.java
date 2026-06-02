package br.com.vrosa.witchcraft.platform;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.UUID;

public interface WPlayer {

    @NotNull UUID uuid();

    @NotNull Locale locale();

    @NotNull WorldRef world();

    @NotNull Vec3 eyePosition();

    @NotNull Vector3f eyeDirection();

    float yaw();

    boolean sneaking();

    @Nullable ToolType heldTool();

    boolean holdingAnyTool();

    void giveTool(@NotNull ToolType tool);

    void actionBar(@NotNull Component message);

    void message(@NotNull Component message);

    void playSound(@NotNull Key sound, float volume, float pitch);

    void sendResourcePack(@NotNull ResourcePackPrompt prompt);
}
