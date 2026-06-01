package br.com.vrosa.witchcraft.erase;

import br.com.vrosa.witchcraft.history.Change;
import br.com.vrosa.witchcraft.history.History;
import br.com.vrosa.witchcraft.keys.ItemDefinition;
import br.com.vrosa.witchcraft.raycast.RayHit;
import br.com.vrosa.witchcraft.raycast.Raycaster;
import br.com.vrosa.witchcraft.render.Segments;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public final class EraserService {

    private static final Key SELECTION_MODEL = Key.key("witchcraft", "selection");

    private static final double DEFAULT_RADIUS = 0.5;
    private static final double MIN_RADIUS = 0.125;
    private static final double MAX_RADIUS = 4.0;
    private static final double RADIUS_STEP = 0.125;

    private static final float SURFACE_OFFSET = 0.01f;
    private static final float EPSILON = 1.0e-4f;
    private static final double ACTIVE_DARKEN = 0.4;

    private final Raycaster raycaster;
    private final History history;

    private final Map<UUID, EraseMode> modes = new HashMap<>();
    private final Map<UUID, Double> radii = new HashMap<>();
    private final Map<UUID, Cursor> cursors = new HashMap<>();
    private final Set<UUID> erasing = new HashSet<>();
    private final Map<UUID, List<Segments.Snapshot>> buffers = new HashMap<>();

    public EraserService(@NotNull Raycaster raycaster, @NotNull History history) {
        this.raycaster = raycaster;
        this.history = history;
    }

    public boolean isNotHolding(@NotNull Player player) {
        return ItemDefinition.getType(player.getInventory().getItemInMainHand()) != ItemDefinition.ERASER;
    }

    public @NotNull EraseMode modeOf(@NotNull Player player) {
        return modes.getOrDefault(player.getUniqueId(), EraseMode.AREA);
    }

    public double radiusOf(@NotNull Player player) {
        return radii.getOrDefault(player.getUniqueId(), DEFAULT_RADIUS);
    }

    public boolean isErasing(@NotNull Player player) {
        return erasing.contains(player.getUniqueId());
    }

    public void tick(@NotNull Player player) {
        if (isNotHolding(player)) {
            if (isErasing(player)) setErasing(player, false);
            removeCursor(player);
            return;
        }

        final var pointer = raycaster.current(player);
        if (pointer == null) {
            removeCursor(player);
            return;
        }

        final var mode = modeOf(player);
        final double radius = radiusOf(player);
        final boolean active = isErasing(player);
        updateCursor(player, pointer.position().getWorld(), pointer, mode, radius, active);
        if (active) eraseStep(player, pointer);
        player.sendActionBar(actionBar(mode, radius, active));
    }

    public void toggleErasing(@NotNull Player player) {
        setErasing(player, !isErasing(player));
    }

    public void cycleMode(@NotNull Player player) {
        final var mode = modeOf(player).next();
        modes.put(player.getUniqueId(), mode);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        player.sendActionBar(actionBar(mode, radiusOf(player), isErasing(player)));
    }

    public void changeRadius(@NotNull Player player, int direction) {
        final double current = radiusOf(player);
        final double next = clamp(current + direction * RADIUS_STEP);
        radii.put(player.getUniqueId(), next);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5f, next > current ? 1.8f : 1.0f);
        player.sendActionBar(actionBar(modeOf(player), next, isErasing(player)));
    }

    public void remove(@NotNull Player player) {
        removeCursor(player);
        modes.remove(player.getUniqueId());
        radii.remove(player.getUniqueId());
        erasing.remove(player.getUniqueId());
        buffers.remove(player.getUniqueId());
    }

    private void setErasing(@NotNull Player player, boolean active) {
        if (active) {
            erasing.add(player.getUniqueId());
            buffers.put(player.getUniqueId(), new ArrayList<>());
            player.playSound(player, Sound.BLOCK_DISPENSER_LAUNCH, 0.6f, 0.8f);
        } else {
            erasing.remove(player.getUniqueId());
            flush(player);
            player.playSound(player, Sound.BLOCK_DISPENSER_LAUNCH, 0.6f, 1.4f);
        }
    }

    private void eraseStep(@NotNull Player player, @NotNull RayHit pointer) {
        final var center = pointer.position();
        final var hits = Segments.within(center, radiusOf(player));
        if (hits.isEmpty()) return;

        final var targets = switch (modeOf(player)) {
            case AREA -> hits;
            case STROKE -> Segments.byStrokes(center,
                    hits.stream().map(Segments::strokeId).collect(Collectors.toSet()));
        };

        final var buffer = buffers.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        for (final var display : targets) {
            if (!display.isValid()) continue;
            buffer.add(Segments.snapshot(display));
            display.remove();
        }
        player.playSound(player, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 0.4f, 1.4f);
    }

    private void flush(@NotNull Player player) {
        final var buffer = buffers.remove(player.getUniqueId());
        if (buffer != null && !buffer.isEmpty()) {
            history.record(player, new Change.Erase(buffer));
        }
    }

    private void updateCursor(@NotNull Player player, @NotNull World world, @NotNull RayHit pointer,
                              @NotNull EraseMode mode, double radius, boolean active) {
        final int tint = active ? darken(mode.tint()) : mode.tint();
        var cursor = cursors.get(player.getUniqueId());
        if (cursor == null || !cursor.display.isValid() || !cursor.display.getWorld().equals(world)) {
            removeCursor(player);
            final var display = world.spawn(pointer.position(), ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setBillboard(Display.Billboard.FIXED);
                d.setTeleportDuration(1);
                d.setItemStack(selectionItem(tint));
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                d.setTransformation(surface(pointer.normal(), radius));
            });
            cursors.put(player.getUniqueId(), new Cursor(display, tint));
            return;
        }

        cursor.display.teleport(pointer.position());
        cursor.display.setTransformation(surface(pointer.normal(), radius));
        if (cursor.tint != tint) {
            cursor.display.setItemStack(selectionItem(tint));
            cursor.tint = tint;
        }
    }

    private void removeCursor(@NotNull Player player) {
        final var cursor = cursors.remove(player.getUniqueId());
        if (cursor != null && cursor.display.isValid()) cursor.display.remove();
    }

    private static @NotNull Transformation surface(@NotNull Vector3f normal, double radius) {
        final var n = new Vector3f(normal);
        if (n.lengthSquared() < EPSILON) n.set(0f, 1f, 0f);
        n.normalize();

        final var rotation = new Quaternionf().rotationTo(new Vector3f(0f, 0f, 1f), n);
        final float diameter = (float) (radius * 2.0);
        final var offset = new Vector3f(n).mul(SURFACE_OFFSET);
        return new Transformation(offset, rotation,
                new Vector3f(diameter, diameter, diameter), new Quaternionf());
    }

    private static @NotNull ItemStack selectionItem(int tint) {
        final var item = ItemStack.of(Material.PAPER);
        item.setData(DataComponentTypes.ITEM_MODEL, SELECTION_MODEL);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                .addColor(Color.fromRGB(tint & 0xFFFFFF))
                .build());
        return item;
    }

    private static @NotNull Component actionBar(@NotNull EraseMode mode, double radius, boolean active) {
        final var state = active
                ? Component.text("APAGANDO", NamedTextColor.RED)
                : Component.text("parado", NamedTextColor.GREEN);
        return Component.text("Borracha", NamedTextColor.GRAY)
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(mode.label(), mode.color()))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format(Locale.ROOT, "Raio %.3f", radius), NamedTextColor.WHITE))
                .append(Component.text("  •  ", NamedTextColor.DARK_GRAY))
                .append(state);
    }

    private static int darken(int rgb) {
        final int r = (int) (((rgb >> 16) & 0xFF) * ACTIVE_DARKEN);
        final int g = (int) (((rgb >> 8) & 0xFF) * ACTIVE_DARKEN);
        final int b = (int) ((rgb & 0xFF) * ACTIVE_DARKEN);
        return (r << 16) | (g << 8) | b;
    }

    private static double clamp(double value) {
        return Math.clamp(value, MIN_RADIUS, MAX_RADIUS);
    }

    private static final class Cursor {
        final ItemDisplay display;
        int tint;

        Cursor(ItemDisplay display, int tint) {
            this.display = display;
            this.tint = tint;
        }
    }
}
