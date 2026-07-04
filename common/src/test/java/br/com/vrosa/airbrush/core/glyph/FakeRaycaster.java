package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.Raycaster;
import br.com.vrosa.airbrush.platform.WPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FakeRaycaster implements Raycaster {

    private final Pose pointer;

    FakeRaycaster(@Nullable Pose pointer) {
        this.pointer = pointer;
    }

    @Override
    public @Nullable Pose current(@NotNull WPlayer player) {
        return pointer;
    }

    @Override
    public @Nullable Pose cast(@NotNull WPlayer player) {
        return pointer;
    }

    @Override
    public @Nullable Pose project(@NotNull br.com.vrosa.airbrush.platform.WorldRef world,
                                  @NotNull br.com.vrosa.airbrush.platform.Vec3 origin,
                                  @NotNull org.joml.Vector3f direction, double maxDistance) {
        return null;
    }

    @Override
    public void clear(@NotNull WPlayer player) {}

    @Override
    public void tick(@NotNull WPlayer player, boolean showCursor) {}
}
