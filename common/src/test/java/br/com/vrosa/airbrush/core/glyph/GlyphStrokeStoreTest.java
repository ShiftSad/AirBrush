package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlyphStrokeStoreTest {

    private static final Vec3 NEAR_CENTER = new Vec3(10, 64, -5);
    private static final Vec3 FAR_CENTER = new Vec3(80, 64, 40);

    @Test
    void expiredStrokesArePruned() {
        final var store = new GlyphStrokeStore();
        store.add(stroke(new FakeSegmentHandle(), System.currentTimeMillis() - 1));

        assertTrue(store.near(StrokeFactory.WORLD, NEAR_CENTER, 10).isEmpty());
    }

    @Test
    void strokesWithInvalidHandlesArePruned() {
        final var store = new GlyphStrokeStore();
        final var handle = new FakeSegmentHandle();
        store.add(stroke(handle, Long.MAX_VALUE));
        handle.invalidate();

        store.prune(System.currentTimeMillis());
        assertTrue(store.near(StrokeFactory.WORLD, NEAR_CENTER, 10).isEmpty());
    }

    @Test
    void nearFiltersByDistanceAndWorld() {
        final var store = new GlyphStrokeStore();
        final var alive = stroke(new FakeSegmentHandle(), Long.MAX_VALUE);
        store.add(alive);

        assertEquals(List.of(alive), store.near(StrokeFactory.WORLD, NEAR_CENTER, 5));
        assertTrue(store.near(StrokeFactory.WORLD, FAR_CENTER, 5).isEmpty());
        assertTrue(store.near(new StrokeFactory.TestWorld("other"), NEAR_CENTER, 5).isEmpty());
    }

    private static GlyphStroke stroke(FakeSegmentHandle handle, long expireAt) {
        final var samples = StrokeFactory.circle(StrokeFactory.floor(), 1.0, 16, 0, 0, 0, 1);
        return new GlyphStroke(UUID.randomUUID(), UUID.randomUUID(), StrokeFactory.WORLD,
                samples, List.of(handle), expireAt);
    }
}
