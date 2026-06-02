package br.com.vrosa.airbrush.paper.config;

import br.com.vrosa.airbrush.core.config.ConfigSource;
import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class PaperConfig {

    private PaperConfig() {}

    public static void load(@NotNull AirBrushConfig config, @NotNull FileConfiguration yaml) {
        config.load(new ConfigSource() {
            @Override
            public double getDouble(@NotNull String key, double fallback) {
                return yaml.getDouble(key, fallback);
            }

            @Override
            public int getInt(@NotNull String key, int fallback) {
                return yaml.getInt(key, fallback);
            }

            @Override
            public boolean getBoolean(@NotNull String key, boolean fallback) {
                return yaml.getBoolean(key, fallback);
            }

            @Override
            public @NotNull String getString(@NotNull String key, @NotNull String fallback) {
                return yaml.getString(key, fallback);
            }
        });
    }
}
