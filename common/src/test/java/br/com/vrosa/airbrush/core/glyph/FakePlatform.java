package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class FakePlatform implements Platform {

    record CraftResult(String recipeId, double quality, Vec3 at, CarriedInk ink) {}

    static final class FakeDrop implements DropHandle {

        private final String itemId;
        private final Vec3 position;
        private int amount;
        String inkId;
        Integer inkColor;
        int durability;

        FakeDrop(String itemId, int amount, Vec3 position) {
            this.itemId = itemId;
            this.amount = amount;
            this.position = position;
        }

        @Override
        public String inkId() {
            return inkId;
        }

        @Override
        public Integer inkColor() {
            return inkColor;
        }

        @Override
        public int durabilityRemaining() {
            return durability;
        }

        @Override
        public @NotNull String itemId() {
            return itemId;
        }

        @Override
        public int amount() {
            return amount;
        }

        @Override
        public @NotNull Vec3 position() {
            return position;
        }

        @Override
        public void consume(int amount) {
            this.amount = Math.max(0, this.amount - amount);
        }
    }

    final List<FakeDrop> drops = new ArrayList<>();
    final List<CraftResult> results = new ArrayList<>();

    @Override
    public @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb,
                                               boolean persistent, boolean bright) {
        return new FakeSegmentHandle();
    }

    @Override
    public @NotNull CursorHandle spawnCursor(@NotNull WorldRef world, @NotNull Vec3 at, int tint,
                                             @NotNull Transform transform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull TileHandle spawnTile(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull Transform transform) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        return List.of();
    }

    @Override
    public @NotNull List<SegmentHandle> segmentsByStrokes(@NotNull WorldRef world, @NotNull Set<UUID> strokeIds) {
        return List.of();
    }

    @Override
    public @NotNull SegmentHandle restore(@NotNull SegmentSnapshot snapshot) {
        return new FakeSegmentHandle();
    }

    @Override
    public @NotNull List<DropHandle> droppedItemsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius) {
        return List.copyOf(drops);
    }

    @Override
    public void dropCraftResult(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull String recipeId, double quality,
                                @Nullable CarriedInk ink) {
        results.add(new CraftResult(recipeId, quality, at, ink));
    }

    @Override
    public void craftEffects(@NotNull WorldRef world, @NotNull Vec3 center) {}

    @Override
    public int cauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block) {
        return -1;
    }

    @Override
    public void setCauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block, int level) {}

    @Override
    public void brewEffects(@NotNull WorldRef world, @NotNull Vec3 center) {}

    @Override
    public void mixEffects(@NotNull WorldRef world, @NotNull Vec3 center) {}

    @Override
    public void storeBrew(@NotNull WorldRef world, @NotNull Vec3 block, @Nullable String inkId) {}
}
