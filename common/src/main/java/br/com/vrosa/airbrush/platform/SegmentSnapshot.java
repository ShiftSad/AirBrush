package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SegmentSnapshot(
        @NotNull WorldRef world,
        @NotNull Vec3 position,
        @NotNull Transform transform,
        @NotNull UUID strokeId,
        @NotNull UUID segmentId,
        int rgb,
        boolean persistent,
        boolean bright,
        @Nullable Vec3 anchor) {
}
