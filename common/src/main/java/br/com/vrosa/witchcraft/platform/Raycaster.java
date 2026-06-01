package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Raycaster {

    @Nullable Pose current(@NotNull WPlayer player);

    void clear(@NotNull WPlayer player);

    void tick(@NotNull WPlayer player, boolean showCursor);
}
