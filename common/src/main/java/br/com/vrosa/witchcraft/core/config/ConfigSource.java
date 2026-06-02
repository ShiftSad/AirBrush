package br.com.vrosa.witchcraft.core.config;

import org.jetbrains.annotations.NotNull;

public interface ConfigSource {

    double getDouble(@NotNull String key, double fallback);

    int getInt(@NotNull String key, int fallback);

    boolean getBoolean(@NotNull String key, boolean fallback);

    @NotNull String getString(@NotNull String key, @NotNull String fallback);
}
