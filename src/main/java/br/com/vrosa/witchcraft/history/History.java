package br.com.vrosa.witchcraft.history;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class History {

    private static final int MAX_CHANGES = 100;

    private final Map<UUID, Deque<Change>> changes = new HashMap<>();

    public void record(@NotNull Player player, @NotNull Change change) {
        final var stack = changes.computeIfAbsent(player.getUniqueId(), _ -> new ArrayDeque<>());
        stack.push(change);
        while (stack.size() > MAX_CHANGES) stack.removeLast();
    }

    public @NotNull Result undo(@NotNull Player player, int count) {
        final var stack = changes.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) return new Result(0, 0);

        int undoneChanges = 0;
        int undoneSegments = 0;
        for (int i = 0; i < count && !stack.isEmpty(); i++) {
            final var change = stack.pop();
            change.revert();
            undoneChanges++;
            undoneSegments += change.size();
        }
        return new Result(undoneChanges, undoneSegments);
    }

    public void clear(@NotNull Player player) {
        changes.remove(player.getUniqueId());
    }

    public record Result(int changes, int segments) {}
}
