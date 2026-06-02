package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record ResourcePackPrompt(@NotNull UUID id, @NotNull String url, @NotNull String hash, boolean required) {
}
