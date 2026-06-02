package br.com.vrosa.airbrush.core.resourcepack;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.platform.ResourcePackPrompt;
import br.com.vrosa.airbrush.platform.WPlayer;
import org.jetbrains.annotations.NotNull;

public final class ResourcePackService {

    private final AirBrushConfig config;
    private ResourcePackServer server;
    private ResourcePackPrompt prompt;

    public ResourcePackService(@NotNull AirBrushConfig config) {
        this.config = config;
    }

    public void start() {
        if (!config.resourcePackEnabled()) return;

        final var archive = ResourcePackArchive.load();
        server = new ResourcePackServer(config.resourcePackIp(), config.resourcePackPort(), archive);
        server.start();
        prompt = new ResourcePackPrompt(archive.id(), server.url(), archive.sha1(), true);
    }

    public void apply(@NotNull WPlayer player) {
        if (prompt != null) player.sendResourcePack(prompt);
    }

    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
        prompt = null;
    }
}
