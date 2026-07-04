package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.SegmentHandle;
import br.com.vrosa.airbrush.platform.SegmentSnapshot;
import br.com.vrosa.airbrush.platform.Transform;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class FakeSegmentHandle implements SegmentHandle {

    private boolean valid = true;
    private boolean persistent;

    void invalidate() {
        valid = false;
    }

    boolean persistent() {
        return persistent;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void remove() {
        valid = false;
    }

    @Override
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    @Override
    public void setTransform(@NotNull Transform transform) {}

    @Override
    public @NotNull WorldRef world() {
        return StrokeFactory.WORLD;
    }

    @Override
    public @NotNull Vec3 position() {
        return new Vec3(0, 0, 0);
    }

    @Override
    public void tag(@NotNull UUID strokeId, @NotNull UUID segmentId, int rgb) {}

    @Override
    public void anchor(@NotNull Vec3 block) {}

    @Override
    public @Nullable Vec3 anchorBlock() {
        return null;
    }

    @Override
    public @Nullable UUID strokeId() {
        return null;
    }

    @Override
    public @Nullable UUID segmentId() {
        return null;
    }

    @Override
    public int color() {
        return 0;
    }

    @Override
    public @NotNull SegmentSnapshot snapshot() {
        throw new UnsupportedOperationException();
    }
}
