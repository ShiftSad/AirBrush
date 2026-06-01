package br.com.vrosa.witchcraft.core.history;

import br.com.vrosa.witchcraft.platform.Platform;
import br.com.vrosa.witchcraft.platform.SegmentSnapshot;
import br.com.vrosa.witchcraft.platform.Vec3;
import br.com.vrosa.witchcraft.platform.WorldRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public sealed interface Change permits Change.Draw, Change.Erase {

    void revert(@NotNull Platform platform);

    int size();

    record Draw(@NotNull WorldRef world, @NotNull Vec3 near, @NotNull UUID strokeId, int size) implements Change {
        @Override
        public void revert(@NotNull Platform platform) {
            for (final var display : platform.segmentsByStrokes(world, Set.of(strokeId))) {
                if (display.isValid()) display.remove();
            }
        }
    }

    record Erase(@NotNull List<SegmentSnapshot> snapshots) implements Change {
        @Override
        public void revert(@NotNull Platform platform) {
            for (final var snapshot : snapshots) platform.restore(snapshot);
        }

        @Override
        public int size() {
            return snapshots.size();
        }
    }
}
