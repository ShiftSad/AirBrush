package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface SegmentHandle {

    boolean isValid();

    void remove();

    void setPersistent(boolean persistent);

    void setTransform(@NotNull Transform transform);

    @NotNull WorldRef world();

    @NotNull Vec3 position();

    void tag(@NotNull UUID strokeId, @NotNull UUID segmentId, int rgb);

    /** Marks the block (block coords) this segment is drawn on; breaking it removes the segment. */
    void anchor(@NotNull Vec3 block);

    @Nullable Vec3 anchorBlock();

    @Nullable UUID strokeId();

    @Nullable UUID segmentId();

    int color();

    @NotNull SegmentSnapshot snapshot();
}
