package br.com.vrosa.airbrush.core.history;

import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.SegmentSnapshot;
import br.com.vrosa.airbrush.platform.Vec3;
import br.com.vrosa.airbrush.platform.WorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public sealed interface Change permits Change.Draw, Change.Erase {

    /** Reverts the change and returns how many segments were affected. */
    int revert(@NotNull Platform platform);

    int size();

    record Draw(@NotNull WorldRef world, @NotNull Vec3 near, @NotNull UUID strokeId, int size) implements Change {
        @Override
        public int revert(@NotNull Platform platform) {
            int removed = 0;
            for (final var display : platform.segmentsByStrokes(world, Set.of(strokeId))) {
                if (!display.isValid()) continue;
                display.remove();
                removed++;
            }
            return removed;
        }
    }

    record Erase(@NotNull List<SegmentSnapshot> snapshots) implements Change {
        @Override
        public int revert(@NotNull Platform platform) {
            int restored = 0;
            for (final var snapshot : snapshots) {
                platform.restore(snapshot);
                restored++;
            }
            return restored;
        }

        @Override
        public int size() {
            return snapshots.size();
        }
    }
}
