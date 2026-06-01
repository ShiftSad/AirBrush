package br.com.vrosa.witchcraft.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractRaycaster implements Raycaster {

    private final Map<UUID, Pose> cache = new ConcurrentHashMap<>();

    @Override
    public @Nullable Pose current(@NotNull WPlayer player) {
        return cache.get(player.uuid());
    }

    @Override
    public void clear(@NotNull WPlayer player) {
        cache.remove(player.uuid());
    }

    @Override
    public void tick(@NotNull WPlayer player, boolean showCursor) {
        if (!player.holdingAnyTool()) {
            cache.remove(player.uuid());
            return;
        }

        final var hit = trace(player);
        if (hit == null) {
            cache.remove(player.uuid());
            return;
        }

        if (showCursor) showCursor(player, hit);
        cache.put(player.uuid(), hit);
    }

    protected abstract @Nullable Pose trace(@NotNull WPlayer player);

    protected abstract void showCursor(@NotNull WPlayer player, @NotNull Pose hit);
}
