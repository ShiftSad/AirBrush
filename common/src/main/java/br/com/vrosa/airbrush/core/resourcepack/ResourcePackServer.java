package br.com.vrosa.airbrush.core.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;

public final class ResourcePackServer {

    private static final String PATH = "/airbrush.zip";

    private final String ip;
    private final int port;
    private final ResourcePackArchive archive;
    private HttpServer server;

    public ResourcePackServer(@NotNull String ip, int port, @NotNull ResourcePackArchive archive) {
        this.ip = ip;
        this.port = port;
        this.archive = archive;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(ip, port), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext(PATH, this::handle);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public @NotNull String url() {
        return "http://" + ip + ":" + port + PATH;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            final byte[] bytes = archive.bytes();
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
