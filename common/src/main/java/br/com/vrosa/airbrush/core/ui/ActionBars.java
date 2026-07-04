package br.com.vrosa.airbrush.core.ui;

import br.com.vrosa.airbrush.platform.WPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinates the action bar: important notices ({@link #notice}) hold the bar for a few
 * seconds, and per-tick status bars ({@link #status}) only write when no notice is active —
 * otherwise the notice would vanish before it could be read.
 */
public final class ActionBars {

    private static final long NOTICE_MILLIS = 2500;

    private final Map<UUID, Long> holds = new HashMap<>();

    public void notice(@NotNull WPlayer player, @NotNull Component message) {
        player.actionBar(message);
        holds.put(player.uuid(), System.currentTimeMillis() + NOTICE_MILLIS);
    }

    public void status(@NotNull WPlayer player, @NotNull Component message) {
        if (idle(player)) player.actionBar(message);
    }

    public boolean idle(@NotNull WPlayer player) {
        final var until = holds.get(player.uuid());
        if (until == null) return true;
        if (System.currentTimeMillis() < until) return false;
        holds.remove(player.uuid());
        return true;
    }

    public void clear(@NotNull WPlayer player) {
        holds.remove(player.uuid());
    }
}
