package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Raycaster {

    double MAX_DISTANCE = 10.0;

    @Nullable Pose current(@NotNull WPlayer player);

    void clear(@NotNull WPlayer player);

    void tick(@NotNull WPlayer player, boolean showCursor);
}
