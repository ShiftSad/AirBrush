package br.com.vrosa.witchcraft.core.resourcepack;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public record ResourcePackArchive(byte @NotNull [] bytes, @NotNull String sha1, @NotNull UUID id) {

    private static final String RESOURCE = "/witchcraft-resourcepack.zip";

    public static @NotNull ResourcePackArchive load() {
        final byte[] bytes;
        try (var in = ResourcePackArchive.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Resource pack ausente no classpath: " + RESOURCE);
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final byte[] digest = sha1(bytes);
        return new ResourcePackArchive(bytes, HexFormat.of().formatHex(digest), UUID.nameUUIDFromBytes(digest));
    }

    private static byte[] sha1(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-1").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
