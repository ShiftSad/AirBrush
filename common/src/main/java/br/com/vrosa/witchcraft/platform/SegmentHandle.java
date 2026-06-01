package br.com.vrosa.witchcraft.platform;

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

    @Nullable UUID strokeId();

    @Nullable UUID segmentId();

    int color();

    @NotNull SegmentSnapshot snapshot();
}
