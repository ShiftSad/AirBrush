package br.com.vrosa.airbrush.core.erase;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.history.Change;
import br.com.vrosa.airbrush.core.history.History;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.platform.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

public final class EraserService {

    private static final float SURFACE_OFFSET = 0.01f;
    private static final float EPSILON = 1.0e-4f;
    private static final double ACTIVE_DARKEN = 0.4;

    private final Platform platform;
    private final Raycaster raycaster;
    private final History history;
    private final AirBrushConfig config;

    private final Map<UUID, EraseMode> modes = new HashMap<>();
    private final Map<UUID, Double> radii = new HashMap<>();
    private final Map<UUID, CursorHandle> cursors = new HashMap<>();
    private final Set<UUID> erasing = new HashSet<>();
    private final Map<UUID, List<SegmentSnapshot>> buffers = new HashMap<>();

    public EraserService(@NotNull Platform platform, @NotNull Raycaster raycaster,
                         @NotNull History history, @NotNull AirBrushConfig config) {
        this.platform = platform;
        this.raycaster = raycaster;
        this.history = history;
        this.config = config;
    }

    public boolean isNotHolding(@NotNull WPlayer player) {
        return player.heldTool() != ToolType.ERASER;
    }

    public @NotNull EraseMode modeOf(@NotNull WPlayer player) {
        return modes.getOrDefault(player.uuid(), EraseMode.AREA);
    }

    public double radiusOf(@NotNull WPlayer player) {
        return radii.getOrDefault(player.uuid(), config.eraserDefaultRadius());
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
        player.actionBar(actionBar(player.locale(), mode, radius, active));
    }

    public void toggleErasing(@NotNull WPlayer player) {
        setErasing(player, !isErasing(player));
    }

    public void cycleMode(@NotNull WPlayer player) {
        final var mode = modeOf(player).next();
        modes.put(player.uuid(), mode);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.6f, 1.4f);
        player.actionBar(actionBar(player.locale(), mode, radiusOf(player), isErasing(player)));
    }

    public void changeRadius(@NotNull WPlayer player, int direction) {
        final double current = radiusOf(player);
        final double next = clamp(current + direction * config.eraserRadiusStep());
        radii.put(player.uuid(), next);
        player.playSound(Sounds.UI_BUTTON_CLICK, 0.5f, next > current ? 1.8f : 1.0f);
        player.actionBar(actionBar(player.locale(), modeOf(player), next, isErasing(player)));
    }

    public void remove(@NotNull WPlayer player) {
        removeCursor(player);
        modes.remove(player.uuid());
        radii.remove(player.uuid());
        erasing.remove(player.uuid());
        buffers.remove(player.uuid());
    }

    private void setErasing(@NotNull WPlayer player, boolean active) {
        if (active) {
            erasing.add(player.uuid());
            buffers.put(player.uuid(), new ArrayList<>());
            player.playSound(Sounds.DISPENSER_LAUNCH, 0.6f, 0.8f);
        } else {
            erasing.remove(player.uuid());
            flush(player);
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

        final var buffer = buffers.computeIfAbsent(player.uuid(), _ -> new ArrayList<>());
        for (final var display : targets) {
            if (!display.isValid()) continue;
            buffer.add(display.snapshot());
            display.remove();
        }
        player.playSound(Sounds.ITEM_FRAME_REMOVE, 0.4f, 1.4f);
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
        final var cursor = cursors.get(player.uuid());
        if (cursor == null || !cursor.isValid() || !cursor.world().equals(pointer.world())) {
            removeCursor(player);
            final var spawned = platform.spawnCursor(pointer.world(), pointer.position(), tint,
                    surface(pointer.normal(), radius));
            cursors.put(player.uuid(), spawned);
            return;
        }

        cursor.moveTo(pointer.position());
        cursor.setTransform(surface(pointer.normal(), radius));
        if (cursor.tint() != tint) cursor.setTint(tint);
    }

    private void removeCursor(@NotNull WPlayer player) {
        final var cursor = cursors.remove(player.uuid());
        if (cursor != null && cursor.isValid()) cursor.remove();
    }

    private double clamp(double value) {
        return Math.clamp(value, config.eraserMinRadius(), config.eraserMaxRadius());
    }

    private static @NotNull Transform surface(@NotNull Vector3f normal, double radius) {
        final var n = new Vector3f(normal);
        if (n.lengthSquared() < EPSILON) n.set(0f, 1f, 0f);
        n.normalize();

        final var rotation = new Quaternionf().rotationTo(new Vector3f(0f, 0f, 1f), n);
        final float diameter = (float) (radius * 2.0);
        final var offset = new Vector3f(n).mul(SURFACE_OFFSET);
        return new Transform(offset, rotation,
                new Vector3f(diameter, diameter, diameter), new Quaternionf());
    }

    private static @NotNull Component actionBar(@NotNull Locale locale, @NotNull EraseMode mode, double radius, boolean active) {
        final var modeLabel = Messages.get(locale, mode == EraseMode.AREA ? Messages.Key.ERASER_AREA : Messages.Key.ERASER_STROKE);
        final var state = active
                ? Component.text(Messages.get(locale, Messages.Key.ERASING), NamedTextColor.RED)
                : Component.text(Messages.get(locale, Messages.Key.IDLE), NamedTextColor.GREEN);
        final var separator = Component.text("  •  ", NamedTextColor.DARK_GRAY);
        return Component.text(Messages.get(locale, Messages.Key.ERASER), NamedTextColor.GRAY)
                .append(separator)
                .append(Component.text(modeLabel, mode.color()))
                .append(separator)
                .append(Component.text(Messages.format(locale, Messages.Key.RADIUS, radius), NamedTextColor.WHITE))
                .append(separator)
                .append(state);
    }

    private static int darken(int rgb) {
        final int r = (int) (((rgb >> 16) & 0xFF) * ACTIVE_DARKEN);
        final int g = (int) (((rgb >> 8) & 0xFF) * ACTIVE_DARKEN);
        final int b = (int) ((rgb & 0xFF) * ACTIVE_DARKEN);
        return (r << 16) | (g << 8) | b;
    }
}
