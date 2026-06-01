package br.com.vrosa.witchcraft.history;

import br.com.vrosa.witchcraft.render.Segments;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public sealed interface Change permits Change.Draw, Change.Erase {

    void revert();

    int size();

    record Draw(@NotNull Location near, @NotNull UUID strokeId, int size) implements Change {
        @Override
        public void revert() {
            for (final var display : Segments.byStrokes(near, Set.of(strokeId))) {
                if (display.isValid()) display.remove();
            }
        }
    }

    record Erase(@NotNull List<Segments.Snapshot> snapshots) implements Change {
        @Override
        public void revert() {
            for (final var snapshot : snapshots) Segments.restore(snapshot);
        }

        @Override
        public int size() {
            return snapshots.size();
        }
    }
}
