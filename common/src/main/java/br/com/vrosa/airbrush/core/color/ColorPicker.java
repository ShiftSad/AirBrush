package br.com.vrosa.airbrush.core.color;

import br.com.vrosa.airbrush.platform.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class ColorPicker {

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

    private static final float BG_OFFSET_X = 0.4f;
    private static final float BG_SCALE_X = 8f;
    private static final float BG_SCALE_Y = 4f;

    private static final int OPAQUE = 0xFF000000;

    private enum Region { NONE, SV, HUE }

    private final Platform platform;
    private final WPlayer player;
    private final WorldRef world;
    private final Vec3 anchorPos;
    private final Vector3f anchor;
    private final Vector3f right;
    private final Vector3f up;
    private final Vector3f normal;
    private final Vec3 openPosition;
    private final Quaternionf rotation;
    private final float cell = SQUARE / SV_STEPS;
    private final float hueCell = SQUARE / HUE_STEPS;
    private final double baseS;
    private final double baseV;

    private final List<TileHandle> svTiles = new ArrayList<>();
    private final List<TileHandle> hueTiles = new ArrayList<>();
    private TileHandle preview;
    private TileHandle hueCursor;

    private double hue;
    private boolean following;
    private int coloredForHue = -1;
    private int lastSvIndex = -1;
    private int lastHueIndex = -1;

    private Region region = Region.NONE;
    private double hoverS;
    private double hoverV;
    private double hoverHue;

    ColorPicker(@NotNull Platform platform, @NotNull WPlayer player, double hue, double saturation, double value) {
        this.platform = platform;
        this.player = player;
        this.world = player.world();
        this.hue = hue;
        this.baseS = saturation;
        this.baseV = value;

        final double yaw = Math.toRadians(player.yaw());
        final var facing = new Vector3f((float) -Math.sin(yaw), 0f, (float) Math.cos(yaw)).normalize();

        final var eye = player.eyePosition().toVector3f();
        final var center = new Vector3f(eye).add(new Vector3f(facing).mul((float) DISTANCE));

        this.normal = new Vector3f(facing).mul(-1f);
        this.up = new Vector3f(0f, 1f, 0f);
        this.right = new Vector3f(up).cross(normal).normalize();

        final float cx = (SQUARE + GAP + HUE_WIDTH) / 2.0f;
        final float cy = SQUARE / 2.0f;
        this.anchor = new Vector3f(center)
                .sub(new Vector3f(right).mul(cx))
                .sub(new Vector3f(up).mul(cy));
        this.anchorPos = new Vec3(anchor.x, anchor.y, anchor.z);
        this.openPosition = player.eyePosition();

        this.rotation = new Quaternionf().setFromNormalized(new Matrix3f(
                right.x, right.y, right.z,
                up.x, up.y, up.z,
                normal.x, normal.y, normal.z));
    }

    void spawn() {
        for (int i = 0; i < SV_STEPS; i++) {
            for (int j = 0; j < SV_STEPS; j++) {
                svTiles.add(tile(baseSvTransform(i, j)));
            }
        }
        for (int k = 0; k < HUE_STEPS; k++) {
            final var tile = tile(baseHueTransform(k));
            tile.setBackgroundColor(OPAQUE | Hsv.toRgb(k * (360.0 / HUE_STEPS), 1, 1));
            hueTiles.add(tile);
        }

        preview = tile(tileTransform(0f, -(PREVIEW_HEIGHT + GAP), SQUARE + GAP + HUE_WIDTH, PREVIEW_HEIGHT, 0f));
        hueCursor = tile(tileTransform(SQUARE + GAP - 0.02f, 0f, HUE_WIDTH + 0.04f, 0.05f, 0.012f));
        hueCursor.setBackgroundColor(0xFFFFFFFF);

        recolorSv();
    }

    void tick() {
        detectHover();
        if (following && region == Region.HUE) hue = hoverHue;

        recolorSv();
        preview.setBackgroundColor(OPAQUE | previewColor());
        positionHueCursor();
        applyHoverScale();
    }

    @Nullable Integer click() {
        if (region == Region.SV) return Hsv.toRgb(hue, hoverS, hoverV);
        if (region == Region.HUE) {
            following = !following;
            return null;
        }
        following = false;
        return null;
    }

    boolean shouldClose() {
        return !world.equals(player.world())
                || player.eyePosition().distanceSquared(openPosition) > MOVE_THRESHOLD_SQ;
    }

    void despawn() {
        svTiles.forEach(this::remove);
        hueTiles.forEach(this::remove);
        remove(preview);
        remove(hueCursor);
    }

    private int previewColor() {
        if (region == Region.SV) return Hsv.toRgb(hue, hoverS, hoverV);
        if (region == Region.HUE) return Hsv.toRgb(hoverHue, 1, 1);
        return Hsv.toRgb(hue, baseS, baseV);
    }

    private void detectHover() {
        region = Region.NONE;

        final var origin = player.eyePosition().toVector3f();
        final var dir = player.eyeDirection();

        final double denom = dir.dot(normal);
        if (Math.abs(denom) < EPSILON) return;

        final double t = new Vector3f(anchor).sub(origin).dot(normal) / denom;
        if (t < 0) return;

        final var rel = new Vector3f(origin).add(new Vector3f(dir).mul((float) t)).sub(anchor);
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
                svTiles.get(index++).setBackgroundColor(OPAQUE | Hsv.toRgb(hue, s, v));
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
            if (lastSvIndex >= 0) interpolate(svTiles.get(lastSvIndex), baseSvTransform(lastSvIndex / SV_STEPS, lastSvIndex % SV_STEPS));
            if (svIndex >= 0) interpolate(svTiles.get(svIndex), enlargedSvTransform(svIndex / SV_STEPS, svIndex % SV_STEPS));
            lastSvIndex = svIndex;
        }
        if (hueIndex != lastHueIndex) {
            if (lastHueIndex >= 0) interpolate(hueTiles.get(lastHueIndex), baseHueTransform(lastHueIndex));
            if (hueIndex >= 0) interpolate(hueTiles.get(hueIndex), enlargedHueTransform(hueIndex));
            lastHueIndex = hueIndex;
        }
    }

    private void positionHueCursor() {
        final float ly = (float) (hue / 360.0 * SQUARE) - 0.025f;
        interpolate(hueCursor, tileTransform(SQUARE + GAP - 0.02f, ly, HUE_WIDTH + 0.04f, 0.05f, 0.012f));
    }

    private @NotNull Transform baseSvTransform(int i, int j) {
        return tileTransform(i * cell, j * cell, cell, cell, 0f);
    }

    private @NotNull Transform enlargedSvTransform(int i, int j) {
        final float w = cell * HOVER_SCALE;
        final float cx = i * cell + cell / 2;
        final float cy = j * cell + cell / 2;
        return tileTransform(cx - w / 2, cy - w / 2, w, w, 0.006f);
    }

    private @NotNull Transform baseHueTransform(int k) {
        return tileTransform(SQUARE + GAP, k * hueCell, HUE_WIDTH, hueCell, 0f);
    }

    private @NotNull Transform enlargedHueTransform(int k) {
        final float w = HUE_WIDTH * 1.4f;
        final float h = hueCell * HOVER_SCALE;
        final float cx = SQUARE + GAP + HUE_WIDTH / 2;
        final float cy = k * hueCell + hueCell / 2;
        return tileTransform(cx - w / 2, cy - h / 2, w, h, 0.006f);
    }

    private @NotNull TileHandle tile(@NotNull Transform transform) {
        final var handle = platform.spawnTile(world, anchorPos, transform);
        handle.setBackgroundColor(OPAQUE);
        return handle;
    }

    private void interpolate(@NotNull TileHandle tile, @NotNull Transform transform) {
        final var old = tile.transform();
        tile.setTransform(transform);
        if (!transform.equals(old)) tile.setInterpolationDelay(0);
    }

    private @NotNull Transform tileTransform(float lx, float ly, float w, float h, float zBias) {
        final var translation = new Vector3f(lx + BG_OFFSET_X * w, ly, zBias);
        rotation.transform(translation);
        final var scale = new Vector3f(BG_SCALE_X * w, BG_SCALE_Y * h, 1f);
        return new Transform(translation, new Quaternionf(rotation), scale, new Quaternionf());
    }

    private void remove(@Nullable TileHandle tile) {
        if (tile != null && tile.isValid()) tile.remove();
    }

    private static double clamp01(double value) {
        return Math.clamp(value, 0, 1);
    }
}
