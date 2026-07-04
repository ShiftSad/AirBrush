package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.SegmentHandle;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record GlyphStroke(@NotNull UUID strokeId, @NotNull UUID owner, @NotNull WorldRef world,
                          @NotNull List<Pose> rawSamples,
                          @NotNull List<SegmentHandle> segments,
                          long expireAt) {

    public GlyphStroke {
        rawSamples = List.copyOf(rawSamples);
        segments = List.copyOf(segments);
    }

    public boolean alive(long now) {
        return now < expireAt && segments.stream().anyMatch(SegmentHandle::isValid);
    }

    public @NotNull Vec3 centroid() {
        double x = 0, y = 0, z = 0;
        for (final var sample : rawSamples) {
            x += sample.position().x();
            y += sample.position().y();
            z += sample.position().z();
        }
        final int n = Math.max(1, rawSamples.size());
        return new Vec3(x / n, y / n, z / n);
    }
}
