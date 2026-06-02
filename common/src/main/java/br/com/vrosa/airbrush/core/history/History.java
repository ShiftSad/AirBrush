package br.com.vrosa.airbrush.core.history;

import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.WPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class History {

    private static final int MAX_CHANGES = 100;

    private final Platform platform;
    private final Map<UUID, Deque<Change>> changes = new HashMap<>();

    public History(@NotNull Platform platform) {
        this.platform = platform;
    }

    public void record(@NotNull WPlayer player, @NotNull Change change) {
        final var stack = changes.computeIfAbsent(player.uuid(), _ -> new ArrayDeque<>());
        stack.push(change);
        while (stack.size() > MAX_CHANGES) stack.removeLast();
    }

    public @NotNull Result undo(@NotNull WPlayer player, int count) {
        final var stack = changes.get(player.uuid());
        if (stack == null || stack.isEmpty()) return new Result(0, 0);

        int undoneChanges = 0;
        int undoneSegments = 0;
        for (int i = 0; i < count && !stack.isEmpty(); i++) {
            final var change = stack.pop();
            change.revert(platform);
            undoneChanges++;
            undoneSegments += change.size();
        }
        return new Result(undoneChanges, undoneSegments);
    }

    public void clear(@NotNull WPlayer player) {
        changes.remove(player.uuid());
    }

    public record Result(int changes, int segments) {}
}
