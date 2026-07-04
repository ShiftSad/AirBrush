package br.com.vrosa.airbrush.core.erase;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.history.Change;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.core.ui.ActionBars;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public final class EraserService {

    private static final double ACTIVE_DARKEN = 0.4;
    private static final Vector3f[] AXES = {
            new Vector3f(1f, 0f, 0f),
            new Vector3f(0f, 1f, 0f),
            new Vector3f(0f, 0f, 1f)
    };

    private final Platform platform;
    private final Raycaster raycaster;
    private final History history;
    private final AirBrushConfig config;
    private final ActionBars bars;

    private final Map<UUID, EraseMode> modes = new HashMap<>();
    private final Map<UUID, Double> radii = new HashMap<>();
    private final Map<UUID, CursorHandle[]> cursors = new HashMap<>();
    private final Set<UUID> erasing = new HashSet<>();
    private final Map<UUID, List<SegmentSnapshot>> buffers = new HashMap<>();
    private final Map<UUID, Double> clothPending = new HashMap<>();

    public EraserService(@NotNull Platform platform, @NotNull Raycaster raycaster,
                         @NotNull History history, @NotNull AirBrushConfig config, @NotNull ActionBars bars) {
        this.platform = platform;
        this.raycaster = raycaster;
        this.history = history;
        this.config = config;
        this.bars = bars;
    }

    public boolean isNotHolding(@NotNull WPlayer player) {
        final var tool = player.heldTool();
        return tool != ToolType.ERASER && tool != ToolType.CLOTH;
    }

    public @NotNull EraseMode modeOf(@NotNull WPlayer player) {
        return modes.getOrDefault(player.uuid(), EraseMode.AREA);
    }

    public double radiusOf(@NotNull WPlayer player) {
        final double stored = radii.getOrDefault(player.uuid(), config.eraserDefaultRadius());
        return Math.clamp(stored, config.eraserMinRadius(), maxRadius(player));
    }

    private double maxRadius(@NotNull WPlayer player) {
        return player.heldTool() == ToolType.CLOTH
                ? config.eraserMaxRadius()
                : config.eraserMaxRadius() * player.heldToolTier();
    }

    public boolean isErasing(@NotNull WPlayer player) {
        return erasing.contains(player.uuid());
    }

    public void tick(@NotNull WPlayer player) {
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
        updateCursor(player, pointer, mode, radius, active);
        if (active) eraseStep(player, pointer);
        bars.status(player, actionBar(player, mode, radius, active));
    }

    public void toggleErasing(@NotNull WPlayer player) {
        if (player.heldTool() == ToolType.ERASER && !player.hasPermission(Permissions.TOOLS_USE)) {
            notifyNoPermission(player);
            return;
        }
        if (player.heldTool() == ToolType.CLOTH && !player.heldClothWet()) {
            notifyDry(player);
            return;
        }
        setErasing(player, !isErasing(player));
    }

    public void cycleMode(@NotNull WPlayer player) {
        if (player.heldTool() == ToolType.ERASER && !player.hasPermission(Permissions.TOOLS_USE)) {
            notifyNoPermission(player);
            return;
        }
        final var mode = modeOf(player).next();
        modes.put(player.uuid(), mode);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.6f, 1.4f);
        player.actionBar(actionBar(player, mode, radiusOf(player), isErasing(player)));
    }

    public void changeRadius(@NotNull WPlayer player, int direction) {
        final double current = radiusOf(player);
        final double next = Math.clamp(current + direction * config.eraserRadiusStep(),
                config.eraserMinRadius(), maxRadius(player));
        radii.put(player.uuid(), next);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.5f, next > current ? 1.8f : 1.0f);
        player.actionBar(actionBar(player, modeOf(player), next, isErasing(player)));
    }

    public void remove(@NotNull WPlayer player) {
        removeCursor(player);
        modes.remove(player.uuid());
        radii.remove(player.uuid());
        erasing.remove(player.uuid());
        flush(player);
        clothPending.remove(player.uuid());
    }

    private void setErasing(@NotNull WPlayer player, boolean active) {
        if (active) {
            erasing.add(player.uuid());
            buffers.put(player.uuid(), new ArrayList<>());
            clothPending.put(player.uuid(), 0.0);
            player.playSound(Sounds.DISPENSER_LAUNCH, 0.6f, 0.8f);
        } else {
            erasing.remove(player.uuid());
            flush(player);
            clothPending.remove(player.uuid());
            player.playSound(Sounds.DISPENSER_LAUNCH, 0.6f, 1.4f);
        }
    }

    private void eraseStep(@NotNull WPlayer player, @NotNull Pose pointer) {
        final var center = pointer.position();
        final var hits = platform.segmentsWithin(pointer.world(), center, radiusOf(player));
        if (hits.isEmpty()) return;

        final var targets = switch (modeOf(player)) {
            case AREA -> hits;
            case STROKE -> platform.segmentsByStrokes(pointer.world(),
                    hits.stream().map(SegmentHandle::strokeId).filter(Objects::nonNull).collect(Collectors.toSet()));
        };

        final var buffer = buffers.computeIfAbsent(player.uuid(), id -> new ArrayList<>());
        final boolean cloth = player.heldTool() == ToolType.CLOTH;
        for (final var display : targets) {
            if (!display.isValid()) continue;
            final var snapshot = display.snapshot();
            // Charge before removing so a dry cloth stops erasing instead of
            // wiping segments it can no longer pay for.
            if (cloth && !chargeCloth(player, Cloth.eraseCost(snapshot.transform(), config.clothBlocksPerDurability()))) {
                setErasing(player, false);
                break;
            }
            buffer.add(snapshot);
            display.remove();
        }
        player.playSound(Sounds.ITEM_FRAME_REMOVE, 0.4f, 1.4f);
    }

    private boolean chargeCloth(@NotNull WPlayer player, double cost) {
        if (!player.heldClothWet() || player.heldItemDurability() <= 1) {
            notifyDry(player);
            player.setClothDry();
            return false;
        }

        double pending = clothPending.getOrDefault(player.uuid(), 0.0) + cost;
        while (pending >= 1.0) {
            if (player.heldItemDurability() <= 1) {
                pending = 0.0;
                clothPending.put(player.uuid(), pending);
                notifyDry(player);
                player.setClothDry();
                return false;
            }
            player.damageHeldItem(1);
            pending -= 1.0;
        }
        clothPending.put(player.uuid(), pending);

        if (player.heldItemDurability() <= 1) {
            player.setClothDry();
            notifyDry(player);
            return false;
        }
        return true;
    }

    private void notifyDry(@NotNull WPlayer player) {
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.CLOTH_DRY), NamedTextColor.RED));
    }

    private void notifyNoPermission(@NotNull WPlayer player) {
        bars.notice(player, Component.text(
                Messages.get(player.locale(), Messages.Key.NO_PERMISSION), NamedTextColor.RED));
    }

    private void flush(@NotNull WPlayer player) {
        final var buffer = buffers.remove(player.uuid());
        if (buffer != null && !buffer.isEmpty()) {
            history.record(player, new Change.Erase(buffer));
        }
    }

    private void updateCursor(@NotNull WPlayer player, @NotNull Pose pointer,
                              @NotNull EraseMode mode, double radius, boolean active) {
        final int tint = active ? darken(mode.tint()) : mode.tint();
        var handles = cursors.get(player.uuid());
        final boolean stale = handles == null || Arrays.stream(handles)
                .anyMatch(cursor -> !cursor.isValid() || !cursor.world().equals(pointer.world()));
        if (stale) {
            removeCursor(player);
            handles = new CursorHandle[AXES.length];
            for (int i = 0; i < AXES.length; i++) {
                handles[i] = platform.spawnCursor(pointer.world(), pointer.position(), tint,
                        axisPlane(AXES[i], radius));
            }
            cursors.put(player.uuid(), handles);
            return;
        }

        for (int i = 0; i < AXES.length; i++) {
            final var cursor = handles[i];
            cursor.moveTo(pointer.position());
            cursor.setTransform(axisPlane(AXES[i], radius));
            if (cursor.tint() != tint) cursor.setTint(tint);
        }
    }

    private void removeCursor(@NotNull WPlayer player) {
        final var handles = cursors.remove(player.uuid());
        if (handles == null) return;
        for (final var cursor : handles) {
            if (cursor.isValid()) cursor.remove();
        }
    }

    private static @NotNull Transform axisPlane(@NotNull Vector3f axis, double radius) {
        final var rotation = new Quaternionf().rotationTo(new Vector3f(0f, 0f, 1f), axis);
        final float diameter = (float) (radius * 2.0);
        return new Transform(new Vector3f(), rotation,
                new Vector3f(diameter, diameter, diameter), new Quaternionf());
    }

    private @NotNull Component actionBar(@NotNull WPlayer player, @NotNull EraseMode mode, double radius, boolean active) {
        return actionBar(player.locale(), player.heldTool(), mode, radius, active, player.heldClothWet());
    }

    private static @NotNull Component actionBar(@NotNull Locale locale, @Nullable ToolType tool,
                                                @NotNull EraseMode mode, double radius, boolean active,
                                                boolean clothWet) {
        final var modeLabel = Messages.get(locale, mode == EraseMode.AREA ? Messages.Key.ERASER_AREA : Messages.Key.ERASER_STROKE);
        final var state = active
                ? Component.text(Messages.get(locale, Messages.Key.ERASING), NamedTextColor.RED)
                : Component.text(Messages.get(locale, Messages.Key.IDLE), NamedTextColor.GREEN);
        final var separator = Component.text("  •  ", NamedTextColor.DARK_GRAY);
        final var toolLabel = tool == ToolType.CLOTH
                ? Component.text(Messages.get(locale, Messages.Key.CLOTH), ToolType.CLOTH.color())
                : Component.text(Messages.get(locale, Messages.Key.ERASER), NamedTextColor.GRAY);
        final var line = toolLabel
                .append(separator)
                .append(Component.text(modeLabel, mode.color()))
                .append(separator)
                .append(Component.text(Messages.format(locale, Messages.Key.RADIUS, radius), NamedTextColor.WHITE))
                .append(separator)
                .append(state);
        if (tool != ToolType.CLOTH) return line;
        return line.append(separator)
                .append(Component.text(Messages.get(locale, clothWet ? Messages.Key.CLOTH_WET : Messages.Key.CLOTH_DRY_STATE),
                        clothWet ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY));
    }

    private static int darken(int rgb) {
        final int r = (int) (((rgb >> 16) & 0xFF) * ACTIVE_DARKEN);
        final int g = (int) (((rgb >> 8) & 0xFF) * ACTIVE_DARKEN);
        final int b = (int) ((rgb & 0xFF) * ACTIVE_DARKEN);
        return (r << 16) | (g << 8) | b;
    }
}
