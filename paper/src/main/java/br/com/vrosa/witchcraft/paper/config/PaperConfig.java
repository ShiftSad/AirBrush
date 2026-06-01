package br.com.vrosa.witchcraft.paper.config;

import br.com.vrosa.witchcraft.core.config.ConfigSource;
import br.com.vrosa.witchcraft.core.config.WitchCraftConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class PaperConfig {

    private PaperConfig() {}

    public static void load(@NotNull WitchCraftConfig config, @NotNull FileConfiguration yaml) {
        config.load(new ConfigSource() {
            @Override
            public double getDouble(@NotNull String key, double fallback) {
                return yaml.getDouble(key, fallback);
            }

            @Override
            public int getInt(@NotNull String key, int fallback) {
                return yaml.getInt(key, fallback);
            }
        });
    }
}
