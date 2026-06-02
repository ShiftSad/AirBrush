package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SegmentSnapshot(
        @NotNull WorldRef world,
        @NotNull Vec3 position,
        @NotNull Transform transform,
        @NotNull UUID strokeId,
        @NotNull UUID segmentId,
        int rgb) {
}
