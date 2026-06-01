package br.com.vrosa.witchcraft.minestom.config;

import br.com.vrosa.witchcraft.core.config.ConfigSource;
import br.com.vrosa.witchcraft.core.config.WitchCraftConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MinestomConfig {

    private static final Path FILE = Path.of("config.properties");

    private MinestomConfig() {}

    public static void load(@NotNull WitchCraftConfig config) {
        if (!Files.exists(FILE)) copyDefault();

        final var properties = new Properties();
        try (var in = Files.newInputStream(FILE)) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        config.load(new ConfigSource() {
            @Override
            public double getDouble(@NotNull String key, double fallback) {
                final var value = properties.getProperty(key);
                if (value == null) return fallback;
                try {
                    return Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    return fallback;
                }
            }

            @Override
            public int getInt(@NotNull String key, int fallback) {
                final var value = properties.getProperty(key);
                if (value == null) return fallback;
                try {
                    return Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    return fallback;
                }
            }
        });
    }

    private static void copyDefault() {
        try (var in = MinestomConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) Files.copy(in, FILE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
