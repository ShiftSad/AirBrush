package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.ResourcePackPrompt;
import br.com.vrosa.airbrush.platform.ToolType;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WPlayer;
import br.com.vrosa.airbrush.platform.WorldRef;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class FakeWPlayer implements WPlayer {

    private final UUID uuid = UUID.randomUUID();
    final List<Component> actionBars = new ArrayList<>();
    int consumedItems;
    ToolType heldTool = ToolType.MARKER;

    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    @Override
    public @NotNull Locale locale() {
        return Locale.US;
    }

    @Override
    public @NotNull WorldRef world() {
        return StrokeFactory.WORLD;
    }

    @Override
    public @NotNull Vec3 eyePosition() {
        return new Vec3(0, 65, 0);
    }

    @Override
    public @NotNull Vector3f eyeDirection() {
        return new Vector3f(0, -1, 0);
    }

    @Override
    public float yaw() {
        return 0;
    }

    @Override
    public boolean sneaking() {
        return false;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }

    @Override
    public @Nullable ToolType heldTool() {
        return heldTool;
    }

    @Override
    public boolean holdingAnyTool() {
        return heldTool != null;
    }

    @Override
    public void giveTool(@NotNull ToolType tool) {}

    @Override
    public void damageHeldItem(int amount) {}

    @Override
    public void repairHeldItem(int amount) {}

    @Override
    public int heldItemDurability() {
        return 100;
    }

    @Override
    public int heldToolTier() {
        return 1;
    }

    @Override
    public double heldToolQuality() {
        return 1.0;
    }

    private Double radius;
    private String ink;
    private Integer inkColor;

    @Override
    public @Nullable Double heldToolRadius() {
        return radius;
    }

    @Override
    public void setHeldToolRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public @Nullable String heldToolInk() {
        return ink;
    }

    @Override
    public @Nullable Integer heldToolInkColor() {
        return inkColor;
    }

    @Override
    public void setHeldToolInk(@NotNull String inkId, int rgb, @NotNull Component inkLore) {
        this.ink = inkId;
        this.inkColor = rgb;
    }

    private boolean clothWet;

    @Override
    public boolean heldClothWet() {
        return clothWet;
    }

    @Override
    public void setClothWet(@NotNull Component solventLore) {
        clothWet = true;
    }

    @Override
    public void setClothDry() {
        clothWet = false;
    }

    @Override
    public void consumeHeldItem() {
        consumedItems++;
    }

    @Override
    public void actionBar(@NotNull Component message) {
        actionBars.add(message);
    }

    @Override
    public void message(@NotNull Component message) {}

    @Override
    public void playSound(@NotNull Key sound, float volume, float pitch) {}

    @Override
    public void sendResourcePack(@NotNull ResourcePackPrompt prompt) {}
}
