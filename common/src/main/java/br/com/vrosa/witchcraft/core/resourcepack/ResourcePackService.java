package br.com.vrosa.witchcraft.core.resourcepack;

import br.com.vrosa.witchcraft.core.config.WitchCraftConfig;
import br.com.vrosa.witchcraft.platform.ResourcePackPrompt;
import br.com.vrosa.witchcraft.platform.WPlayer;
import org.jetbrains.annotations.NotNull;

public final class ResourcePackService {

    private final WitchCraftConfig config;
    private ResourcePackServer server;
    private ResourcePackPrompt prompt;

    public ResourcePackService(@NotNull WitchCraftConfig config) {
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
