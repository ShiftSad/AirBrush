package br.com.vrosa.witchcraft.color;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.space;

final class ColorPicker {

    private static final Matrix4f TEXT_BG = new Matrix4f().translate(0.4f, 0f, 0f).scale(8f, 4f, 1f);

    private static final int SV_STEPS = 14;
    private static final int HUE_STEPS = 18;
    private static final float SQUARE = 1.0f;
    private static final float HUE_WIDTH = 0.16f;
    private static final float GAP = 0.08f;
    private static final float PREVIEW_HEIGHT = 0.18f;
    private static final float HOVER_SCALE = 1.9f;
    private static final double DISTANCE = 1.6;
    private static final double MOVE_THRESHOLD_SQ = 0.35 * 0.35;
    private static final float EPSILON = 1.0e-5f;

    private enum Region { NONE, SV, HUE }

    private final Player player;
    private final World world;
    private final Location anchorLocation;
    private final Vector anchor;
    private final Vector right;
    private final Vector up;
    private final Vector normal;
    private final Vector openPosition;
    private final Quaternionf rotation;
    private final float cell = SQUARE / SV_STEPS;
    private final float hueCell = SQUARE / HUE_STEPS;
    private final double baseS;
    private final double baseV;

    private final List<TextDisplay> svTiles = new ArrayList<>();
    private final List<TextDisplay> hueTiles = new ArrayList<>();
    private TextDisplay preview;
    private TextDisplay hueCursor;

    private double hue;
    private boolean following;
    private int coloredForHue = -1;
    private int lastSvIndex = -1;
    private int lastHueIndex = -1;

    private Region region = Region.NONE;
    private double hoverS;
    private double hoverV;
    private double hoverHue;

    ColorPicker(@NotNull Player player, double hue, double saturation, double value) {
        this.player = player;
        this.world = player.getWorld();
        this.hue = hue;
        this.baseS = saturation;
        this.baseV = value;

        final double yaw = Math.toRadians(player.getLocation().getYaw());
        final var facing = new Vector(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();

        final var eye = player.getEyeLocation().toVector();
        final var center = eye.clone().add(facing.clone().multiply(DISTANCE));

        this.normal = facing.clone().multiply(-1);
        this.up = new Vector(0, 1, 0);
        this.right = up.getCrossProduct(normal).normalize();

        final double cx = (SQUARE + GAP + HUE_WIDTH) / 2.0;
        final double cy = SQUARE / 2.0;
        this.anchor = center.clone()
                .subtract(right.clone().multiply(cx))
                .subtract(up.clone().multiply(cy));
        this.anchorLocation = anchor.toLocation(world);
        this.openPosition = player.getLocation().toVector();

        this.rotation = new Quaternionf().setFromNormalized(new Matrix3f(
                (float) right.getX(), (float) right.getY(), (float) right.getZ(),
                (float) up.getX(), (float) up.getY(), (float) up.getZ(),
                (float) normal.getX(), (float) normal.getY(), (float) normal.getZ()));
    }

    void spawn() {
        for (int i = 0; i < SV_STEPS; i++) {
            for (int j = 0; j < SV_STEPS; j++) {
                svTiles.add(tile(baseSvMatrix(i, j)));
            }
        }
        for (int k = 0; k < HUE_STEPS; k++) {
            final var tile = tile(baseHueMatrix(k));
            tile.setBackgroundColor(Hsv.toColor(k * (360.0 / HUE_STEPS), 1, 1));
            hueTiles.add(tile);
        }

        preview = tile(matrix(0f, -(PREVIEW_HEIGHT + GAP), SQUARE + GAP + HUE_WIDTH, PREVIEW_HEIGHT, 0f));
        hueCursor = tile(matrix(SQUARE + GAP - 0.02f, 0f, HUE_WIDTH + 0.04f, 0.05f, 0.012f));
        hueCursor.setBackgroundColor(Color.WHITE);

        recolorSv();
    }

    void tick() {
        detectHover();
        if (following && region == Region.HUE) hue = hoverHue;

        recolorSv();
        preview.setBackgroundColor(previewColor());
        positionHueCursor();
        applyHoverScale();
    }

    @Nullable Color click() {
        if (region == Region.SV) return Hsv.toColor(hue, hoverS, hoverV);
        if (region == Region.HUE) {
            following = !following;
            return null;
        }
        following = false;
        return null;
    }

    boolean shouldClose() {
        return !world.equals(player.getWorld())
                || player.getLocation().toVector().distanceSquared(openPosition) > MOVE_THRESHOLD_SQ;
    }

    void despawn() {
        svTiles.forEach(this::remove);
        hueTiles.forEach(this::remove);
        remove(preview);
        remove(hueCursor);
    }

    private @NotNull Color previewColor() {
        if (region == Region.SV) return Hsv.toColor(hue, hoverS, hoverV);
        if (region == Region.HUE) return Hsv.toColor(hoverHue, 1, 1);
        return Hsv.toColor(hue, baseS, baseV);
    }

    private void detectHover() {
        region = Region.NONE;

        final var eye = player.getEyeLocation();
        final var origin = eye.toVector();
        final var dir = eye.getDirection();

        final double denom = dir.dot(normal);
        if (Math.abs(denom) < EPSILON) return;

        final double t = anchor.clone().subtract(origin).dot(normal) / denom;
        if (t < 0) return;

        final var rel = origin.clone().add(dir.clone().multiply(t)).subtract(anchor);
        final double lx = rel.dot(right);
        final double ly = rel.dot(up);

        if (lx >= 0 && lx <= SQUARE && ly >= 0 && ly <= SQUARE) {
            region = Region.SV;
            hoverS = clamp01(lx / SQUARE);
            hoverV = clamp01(ly / SQUARE);
        } else if (lx >= SQUARE + GAP && lx <= SQUARE + GAP + HUE_WIDTH && ly >= 0 && ly <= SQUARE) {
            region = Region.HUE;
            hoverHue = clamp01(ly / SQUARE) * 360.0;
        }
    }

    private void recolorSv() {
        if (coloredForHue == (int) hue) return;
        coloredForHue = (int) hue;

        int index = 0;
        for (int i = 0; i < SV_STEPS; i++) {
            for (int j = 0; j < SV_STEPS; j++) {
                final double s = i / (double) (SV_STEPS - 1);
                final double v = j / (double) (SV_STEPS - 1);
                svTiles.get(index++).setBackgroundColor(Hsv.toColor(hue, s, v));
            }
        }
    }

    private void applyHoverScale() {
        int svIndex = -1;
        int hueIndex = -1;
        if (region == Region.SV) {
            final int i = (int) Math.round(hoverS * (SV_STEPS - 1));
            final int j = (int) Math.round(hoverV * (SV_STEPS - 1));
            svIndex = i * SV_STEPS + j;
        } else if (region == Region.HUE) {
            hueIndex = Math.clamp((int) Math.round(hoverHue / 360.0 * HUE_STEPS), 0, HUE_STEPS - 1);
        }

        if (svIndex != lastSvIndex) {
            if (lastSvIndex >= 0) interpolate(svTiles.get(lastSvIndex), baseSvMatrix(lastSvIndex / SV_STEPS, lastSvIndex % SV_STEPS));
            if (svIndex >= 0) interpolate(svTiles.get(svIndex), enlargedSvMatrix(svIndex / SV_STEPS, svIndex % SV_STEPS));
            lastSvIndex = svIndex;
        }
        if (hueIndex != lastHueIndex) {
            if (lastHueIndex >= 0) interpolate(hueTiles.get(lastHueIndex), baseHueMatrix(lastHueIndex));
            if (hueIndex >= 0) interpolate(hueTiles.get(hueIndex), enlargedHueMatrix(hueIndex));
            lastHueIndex = hueIndex;
        }
    }

    private void positionHueCursor() {
        final float ly = (float) (hue / 360.0 * SQUARE) - 0.025f;
        interpolate(hueCursor, matrix(SQUARE + GAP - 0.02f, ly, HUE_WIDTH + 0.04f, 0.05f, 0.012f));
    }

    private @NotNull Matrix4f baseSvMatrix(int i, int j) {
        return matrix(i * cell, j * cell, cell, cell, 0f);
    }

    private @NotNull Matrix4f enlargedSvMatrix(int i, int j) {
        final float w = cell * HOVER_SCALE;
        final float cx = i * cell + cell / 2;
        final float cy = j * cell + cell / 2;
        return matrix(cx - w / 2, cy - w / 2, w, w, 0.006f);
    }

    private @NotNull Matrix4f baseHueMatrix(int k) {
        return matrix(SQUARE + GAP, k * hueCell, HUE_WIDTH, hueCell, 0f);
    }

    private @NotNull Matrix4f enlargedHueMatrix(int k) {
        final float w = HUE_WIDTH * 1.4f;
        final float h = hueCell * HOVER_SCALE;
        final float cx = SQUARE + GAP + HUE_WIDTH / 2;
        final float cy = k * hueCell + hueCell / 2;
        return matrix(cx - w / 2, cy - h / 2, w, h, 0.006f);
    }

    private @NotNull TextDisplay tile(@NotNull Matrix4f transform) {
        return world.spawn(anchorLocation, TextDisplay.class, d -> {
            d.text(space());
            d.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
            d.setBrightness(new Display.Brightness(15, 15));
            d.setBillboard(Display.Billboard.FIXED);
            d.setSeeThrough(false);
            d.setShadowed(false);
            d.setPersistent(false);
            d.setInterpolationDuration(1);
            d.setTransformationMatrix(transform);
        });
    }

    private void interpolate(@NotNull TextDisplay display, @NotNull Matrix4f transform) {
        final var old = display.getTransformation();
        display.setTransformationMatrix(transform);
        if (!display.getTransformation().equals(old)) display.setInterpolationDelay(0);
    }

    private @NotNull Matrix4f matrix(float lx, float ly, float w, float h, float zBias) {
        return new Matrix4f()
                .rotate(rotation)
                .translate(lx, ly, zBias)
                .scale(w, h, 1f)
                .mul(TEXT_BG);
    }

    private void remove(@Nullable TextDisplay display) {
        if (display != null && display.isValid()) display.remove();
    }

    private static double clamp01(double value) {
        return Math.clamp(value, 0, 1);
    }
}
