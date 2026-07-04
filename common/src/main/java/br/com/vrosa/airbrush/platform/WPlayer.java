package br.com.vrosa.airbrush.platform;

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

    boolean hasPermission(@NotNull String permission);

    @Nullable ToolType heldTool();

    boolean holdingAnyTool();

    void giveTool(@NotNull ToolType tool);

    void damageHeldItem(int amount);

    void repairHeldItem(int amount);

    int heldItemDurability();

    int heldToolTier();

    /** Craft-glyph quality of the held tool (0-1; 1.0 when absent). */
    double heldToolQuality();

    /** Stroke radius saved on the held item, or {@code null} if never adjusted. */
    @Nullable Double heldToolRadius();

    void setHeldToolRadius(double radius);

    /** Ink id loaded in the held item, or {@code null} if never refilled. */
    @Nullable String heldToolInk();

    @Nullable Integer heldToolInkColor();

    /** Writes the ink onto the item and replaces the 2nd lore line with its description. */
    void setHeldToolInk(@NotNull String inkId, int rgb, @NotNull Component inkLore);

    boolean heldClothWet();

    /** Soaks the cloth: swaps the model, flags the solvent and refreshes the lore. */
    void setClothWet(@NotNull Component solventLore);

    void setClothDry();

    void consumeHeldItem();

    void actionBar(@NotNull Component message);

    void message(@NotNull Component message);

    void playSound(@NotNull Key sound, float volume, float pitch);

    void sendResourcePack(@NotNull ResourcePackPrompt prompt);
}
