package br.com.vrosa.airbrush.core.ink;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.ui.ActionBars;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CauldronService {

    /** Width of the tint quad; the cauldron's inner opening is 12/16 = 0.75 on a side. */
    private static final float COVER = 0.75f;
    private static final int WATER_ALPHA = 0x64;
    private static final double SCAN_RADIUS = 0.8;
    // A text display background with a single space covers 1/8 x 1/4 of a block per scale
    // unit, offset 0.4*w from the center — same constants as the ColorPicker.
    private static final float BG_OFFSET_X = 0.4f;
    private static final float BG_SCALE_X = 8f;
    private static final float BG_SCALE_Y = 4f;
    private static final String SOLVENT_STORAGE_PREFIX = "solvent:";

    /**
     * Wait after the last item drops before brewing the ink, so that multi-ingredient
     * recipes (luminous) don't lose to recipes that are a subset of them (amethyst) just
     * because of the order items were thrown in.
     */
    private static final long BREW_GRACE_MS = 1500;
    private static final long REPLACE_CONFIRM_MS = 5000;

    private record BrewKey(@NotNull WorldRef world, int x, int y, int z) {}

    private enum BrewKind { INK, SOLVENT }

    private static final class Brew {
        final BrewKind kind;
        final InkType ink;
        final SolventType solvent;
        final TileHandle tile;
        int color;

        Brew(@NotNull InkType ink, @NotNull TileHandle tile, int color) {
            this.kind = BrewKind.INK;
            this.ink = ink;
            this.solvent = null;
            this.tile = tile;
            this.color = color;
        }

        Brew(@NotNull SolventType solvent, @NotNull TileHandle tile) {
            this.kind = BrewKind.SOLVENT;
            this.ink = null;
            this.solvent = solvent;
            this.tile = tile;
            this.color = solvent.waterRgb();
        }

        int displayColor() {
            return color;
        }
    }

    private record PendingReplace(@NotNull BrewKey key, long expiresAt) {}

    private final Platform platform;
    private final AirBrushConfig config;
    private final ActionBars bars;
    private final Map<BrewKey, Brew> brews = new HashMap<>();
    private final Map<BrewKey, Long> pending = new HashMap<>();
    private final Map<UUID, PendingReplace> replaceConfirms = new HashMap<>();

    public CauldronService(@NotNull Platform platform, @NotNull AirBrushConfig config, @NotNull ActionBars bars) {
        this.platform = platform;
        this.config = config;
        this.bars = bars;
    }

    public boolean isBrew(@NotNull WorldRef world, @NotNull Vec3 block) {
        return brews.containsKey(key(world, block));
    }

    /** An item dropped into the cauldron: mixes dyes into an existing brew or schedules a new one. */
    public boolean deposit(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var key = key(world, block);
        final var brew = brews.get(key);
        if (brew != null) return brew.kind == BrewKind.INK && mixDyes(key, brew);

        if (platform.cauldronWaterLevel(world, block) <= 0) return false;
        pending.put(key, System.currentTimeMillis() + BREW_GRACE_MS);
        return true;
    }

    public void tick() {
        if (pending.isEmpty()) return;
        final long now = System.currentTimeMillis();
        final var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            if (now < entry.getValue()) continue;
            iterator.remove();
            brew(entry.getKey());
        }
    }

    /** Shows the cauldron liquid the player is looking at on the action bar. */
    public void gazeTick(@NotNull WPlayer player, @Nullable Pose pointer) {
        if (pointer == null) return;
        final var n = pointer.normal();
        final var position = pointer.position();
        final var block = new Vec3(position.x() - n.x * 0.05, position.y() - n.y * 0.05, position.z() - n.z * 0.05);
        final var brew = brews.get(key(pointer.world(), block));
        if (brew == null) return;

        final int level = Math.max(0, platform.cauldronWaterLevel(pointer.world(), block));
        final var label = brew.kind == BrewKind.SOLVENT
                ? Messages.solventName(player.locale(), brew.solvent)
                : Messages.inkName(player.locale(), brew.ink);
        bars.status(player, Component.text(label, TextColor.color(brew.displayColor()))
                .append(Component.text("  " + "●".repeat(level) + "○".repeat(Math.max(0, 3 - level)),
                        NamedTextColor.DARK_GRAY)));
    }

    private void brew(@NotNull BrewKey key) {
        if (brews.containsKey(key)) return;

        final var world = key.world();
        final var block = new Vec3(key.x(), key.y(), key.z());
        final int level = platform.cauldronWaterLevel(world, block);
        if (level <= 0) return;

        final var drops = dropsInside(world, key);
        final var available = new HashMap<String, Integer>();
        for (final var drop : drops) available.merge(drop.itemId(), drop.amount(), Integer::sum);

        final var ink = InkType.match(available).orElse(null);
        if (ink != null) {
            consume(drops, ink.ingredients());
            final var brew = new Brew(ink, spawnTint(key, ink.waterRgb(), level), ink.waterRgb());
            brews.put(key, brew);
            store(key);
            platform.brewEffects(world, center(key));
            mixDyes(key, brew);
            return;
        }

        final var solvent = SolventType.match(available).orElse(null);
        if (solvent == null) return;

        consume(drops, solvent.ingredients());
        final var brew = new Brew(solvent, spawnTint(key, solvent.waterRgb(), level));
        brews.put(key, brew);
        store(key);
        platform.brewEffects(world, center(key));
    }

    /** Consumes dyes floating in the cauldron and pulls the ink color toward them. */
    private boolean mixDyes(@NotNull BrewKey key, @NotNull Brew brew) {
        boolean mixed = false;
        for (final var drop : dropsInside(key.world(), key)) {
            final var dye = Dyes.colorOf(drop.itemId());
            if (dye == null) continue;
            for (int i = 0; i < drop.amount(); i++) brew.color = Dyes.mix(brew.color, dye);
            drop.consume(drop.amount());
            mixed = true;
        }
        if (!mixed) return false;

        if (brew.tile.isValid()) brew.tile.setBackgroundColor((WATER_ALPHA << 24) | brew.color);
        store(key);
        platform.mixEffects(key.world(), center(key));
        return true;
    }

    public boolean dip(@NotNull WPlayer player, @NotNull WorldRef world, @NotNull Vec3 block) {
        final var key = key(world, block);
        final var brew = brews.get(key);
        if (brew == null) return false;

        final var tool = player.heldTool();
        if (tool == ToolType.MARKER) return dipMarker(player, world, block, brew);
        if (tool == ToolType.QUILL) return dipQuill(player, world, block, brew);
        if (tool == ToolType.CLOTH) return dipCloth(player, world, block, brew);
        return false;
    }

    private boolean dipMarker(@NotNull WPlayer player, @NotNull WorldRef world, @NotNull Vec3 block, @NotNull Brew brew) {
        if (brew.kind != BrewKind.INK || brew.ink != InkType.AMETHYST) {
            wrongLiquid(player);
            return false;
        }
        return finishInkDip(player, world, block, brew);
    }

    private boolean dipQuill(@NotNull WPlayer player, @NotNull WorldRef world, @NotNull Vec3 block, @NotNull Brew brew) {
        if (brew.kind != BrewKind.INK || brew.ink == InkType.AMETHYST) {
            wrongLiquid(player);
            return false;
        }

        final boolean replacing = isDifferentInk(player, brew);
        if (replacing && !confirmedReplace(player, key(world, block))) {
            replaceConfirms.put(player.uuid(), new PendingReplace(key(world, block), System.currentTimeMillis() + REPLACE_CONFIRM_MS));
            player.playSound(Sounds.UI_BUTTON_CLICK, 0.6f, 1.0f);
            bars.notice(player, Component.text(
                    Messages.get(player.locale(), Messages.Key.CAULDRON_REPLACE), NamedTextColor.YELLOW));
            return true;
        }
        replaceConfirms.remove(player.uuid());

        if (replacing) player.damageHeldItem(Math.max(0, player.heldItemDurability() - 1));
        return finishInkDip(player, world, block, brew);
    }

    private boolean dipCloth(@NotNull WPlayer player, @NotNull WorldRef world, @NotNull Vec3 block, @NotNull Brew brew) {
        if (brew.kind != BrewKind.SOLVENT) {
            wrongLiquid(player);
            return false;
        }
        if (!takeLiquid(world, block)) return false;

        player.setClothWet(brew.solvent.loreLine());
        player.repairHeldItem(config.cauldronClothChargePerDip());
        player.playSound(Sounds.BOTTLE_FILL, 1.0f, 0.9f);
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.CAULDRON_SOAKED), TextColor.color(brew.displayColor())));
        return true;
    }

    private boolean finishInkDip(@NotNull WPlayer player, @NotNull WorldRef world, @NotNull Vec3 block, @NotNull Brew brew) {
        final int color = brew.color;
        if (!takeLiquid(world, block)) return false;

        player.setHeldToolInk(brew.ink.id(), color, brew.ink.loreLine(color));
        player.repairHeldItem(config.cauldronChargePerDip());
        player.playSound(Sounds.BOTTLE_FILL, 1.0f, 1.2f);
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.CAULDRON_RECHARGED), TextColor.color(color)));
        return true;
    }

    private void wrongLiquid(@NotNull WPlayer player) {
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.6f, 0.6f);
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.CAULDRON_WRONG_LIQUID), NamedTextColor.YELLOW));
    }

    private boolean isDifferentInk(@NotNull WPlayer player, @NotNull Brew brew) {
        final var currentInk = player.heldToolInk();
        if (currentInk == null || player.heldItemDurability() <= 1) return false;

        final var currentColor = player.heldToolInkColor();
        return !currentInk.equals(brew.ink.id())
                || currentColor == null || currentColor != brew.color;
    }

    private boolean confirmedReplace(@NotNull WPlayer player, @NotNull BrewKey key) {
        final var pending = replaceConfirms.get(player.uuid());
        return pending != null && pending.key().equals(key)
                && System.currentTimeMillis() < pending.expiresAt();
    }

    private boolean takeLiquid(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var key = key(world, block);
        final var brew = brews.get(key);
        if (brew == null) return false;

        final int level = platform.cauldronWaterLevel(world, block);
        if (level <= 0 || !brew.tile.isValid()) {
            invalidate(world, block);
            return false;
        }

        final int next = level - 1;
        platform.setCauldronWaterLevel(world, block, next);
        if (next <= 0) invalidate(world, block);
        else brew.tile.setTransform(surfaceTransform(next));
        return true;
    }

    public void invalidate(@NotNull WorldRef world, @NotNull Vec3 block) {
        unload(world, block);
        platform.storeBrew(world, block, null);
    }

    /** Recreates the tint tile of a persisted brew when the chunk reloads. */
    public void restore(@NotNull WorldRef world, @NotNull Vec3 block, @NotNull String data) {
        if (data.startsWith(SOLVENT_STORAGE_PREFIX)) {
            restoreSolvent(world, block, data.substring(SOLVENT_STORAGE_PREFIX.length()));
            return;
        }
        restoreInk(world, block, data);
    }

    private void restoreInk(@NotNull WorldRef world, @NotNull Vec3 block, @NotNull String data) {
        final var separator = data.indexOf(';');
        final var inkId = separator < 0 ? data : data.substring(0, separator);
        final var type = InkType.byId(inkId);
        if (type == null) return;

        int color = type.waterRgb();
        if (separator >= 0) {
            try {
                color = Integer.parseInt(data.substring(separator + 1), 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
            }
        }

        final var key = key(world, block);
        final var existing = brews.get(key);
        if (existing != null && existing.tile.isValid()) return;

        final int level = platform.cauldronWaterLevel(world, block);
        if (level <= 0) {
            invalidate(world, block);
            return;
        }
        brews.put(key, new Brew(type, spawnTint(key, color, level), color));
    }

    private void restoreSolvent(@NotNull WorldRef world, @NotNull Vec3 block, @NotNull String solventId) {
        final var type = SolventType.byId(solventId);
        if (type == null) return;

        final var key = key(world, block);
        final var existing = brews.get(key);
        if (existing != null && existing.tile.isValid()) return;

        final int level = platform.cauldronWaterLevel(world, block);
        if (level <= 0) {
            invalidate(world, block);
            return;
        }
        brews.put(key, new Brew(type, spawnTint(key, type.waterRgb(), level)));
    }

    /** Discards the brew from memory (chunk unloading) without deleting the persisted record. */
    public void unload(@NotNull WorldRef world, @NotNull Vec3 block) {
        final var key = key(world, block);
        pending.remove(key);
        final var brew = brews.remove(key);
        if (brew != null && brew.tile.isValid()) brew.tile.remove();
    }

    public void clearPlayer(@NotNull WPlayer player) {
        replaceConfirms.remove(player.uuid());
    }

    /** Removes brew tiles from memory without deleting the chunks' PDC. */
    public void shutdown() {
        pending.clear();
        replaceConfirms.clear();
        for (final var brew : brews.values()) {
            if (brew.tile.isValid()) brew.tile.remove();
        }
        brews.clear();
    }

    private void store(@NotNull BrewKey key) {
        final var brew = brews.get(key);
        if (brew == null) return;
        final String stored = brew.kind == BrewKind.SOLVENT
                ? SOLVENT_STORAGE_PREFIX + brew.solvent.id()
                : brew.ink.id() + ";" + String.format(Locale.ROOT, "%06X", brew.color);
        platform.storeBrew(key.world(), new Vec3(key.x(), key.y(), key.z()), stored);
    }

    private @NotNull TileHandle spawnTint(@NotNull BrewKey key, int color, int level) {
        final var tile = platform.spawnTile(key.world(), surfaceCenter(key), surfaceTransform(level));
        tile.setBackgroundColor((WATER_ALPHA << 24) | color);
        return tile;
    }

    private @NotNull List<DropHandle> dropsInside(@NotNull WorldRef world, @NotNull BrewKey key) {
        return platform.droppedItemsWithin(world, center(key), SCAN_RADIUS).stream()
                .filter(drop -> {
                    final var at = drop.position();
                    return (int) Math.floor(at.x()) == key.x()
                            && (int) Math.floor(at.y()) == key.y()
                            && (int) Math.floor(at.z()) == key.z();
                })
                .toList();
    }

    private static void consume(@NotNull List<DropHandle> drops, @NotNull Map<String, Integer> ingredients) {
        for (final var entry : ingredients.entrySet()) {
            int remaining = entry.getValue();
            for (final var drop : drops) {
                if (remaining <= 0) break;
                if (!drop.itemId().equals(entry.getKey())) continue;
                final int take = Math.min(remaining, drop.amount());
                drop.consume(take);
                remaining -= take;
            }
        }
    }

    private static @NotNull BrewKey key(@NotNull WorldRef world, @NotNull Vec3 block) {
        return new BrewKey(world, (int) Math.floor(block.x()), (int) Math.floor(block.y()), (int) Math.floor(block.z()));
    }

    private static @NotNull Vec3 center(@NotNull BrewKey key) {
        return new Vec3(key.x() + 0.5, key.y() + 0.6, key.z() + 0.5);
    }

    private static @NotNull Vec3 surfaceCenter(@NotNull BrewKey key) {
        return new Vec3(key.x() + 0.5, key.y(), key.z() + 0.5);
    }

    private static @NotNull Transform surfaceTransform(int level) {
        final var rotation = new Quaternionf().rotationX((float) Math.toRadians(-90.0));
        final var translation = new Vector3f(-COVER / 2f + BG_OFFSET_X * COVER, -COVER / 2f, 0f);
        rotation.transform(translation);
        translation.y += waterHeight(level) + 0.02f;
        return new Transform(translation, rotation,
                new Vector3f(BG_SCALE_X * COVER, BG_SCALE_Y * COVER, 1f), new Quaternionf());
    }

    private static float waterHeight(int level) {
        return (6 + level * 3) / 16f;
    }
}
