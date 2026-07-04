package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public interface Raycaster {

    @Nullable Pose current(@NotNull WPlayer player);

    @Nullable Pose cast(@NotNull WPlayer player);

    /** Traces an arbitrary ray against blocks; {@code null} when nothing is hit. */
    @Nullable Pose project(@NotNull WorldRef world, @NotNull Vec3 origin, @NotNull Vector3f direction,
                           double maxDistance);

    void clear(@NotNull WPlayer player);

    void tick(@NotNull WPlayer player, boolean showCursor);
}
