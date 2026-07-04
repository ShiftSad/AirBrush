package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface Platform {

    @NotNull SegmentHandle spawnSegment(@NotNull WorldRef world, @NotNull Vec3 at, int rgb, boolean persistent, boolean bright);

    @NotNull CursorHandle spawnCursor(@NotNull WorldRef world, @NotNull Vec3 at, int tint, @NotNull Transform transform);

    @NotNull TileHandle spawnTile(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull Transform transform);

    @NotNull List<SegmentHandle> segmentsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius);

    @NotNull List<SegmentHandle> segmentsByStrokes(@NotNull WorldRef world, @NotNull Set<UUID> strokeIds);

    @NotNull SegmentHandle restore(@NotNull SegmentSnapshot snapshot);

    @NotNull List<DropHandle> droppedItemsWithin(@NotNull WorldRef world, @NotNull Vec3 center, double radius);

    void dropCraftResult(@NotNull WorldRef world, @NotNull Vec3 at, @NotNull String recipeId, double quality,
                         @Nullable CarriedInk ink);

    void craftEffects(@NotNull WorldRef world, @NotNull Vec3 center);

    /** Cauldron water level at {@code block} (block coords): 1-3, 0 if empty, -1 if not a cauldron. */
    int cauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block);

    void setCauldronWaterLevel(@NotNull WorldRef world, @NotNull Vec3 block, int level);

    void brewEffects(@NotNull WorldRef world, @NotNull Vec3 center);

    void mixEffects(@NotNull WorldRef world, @NotNull Vec3 center);

    /** Persists the cauldron's ink id at {@code block} ({@code null} clears the record). */
    void storeBrew(@NotNull WorldRef world, @NotNull Vec3 block, @Nullable String inkId);
}
