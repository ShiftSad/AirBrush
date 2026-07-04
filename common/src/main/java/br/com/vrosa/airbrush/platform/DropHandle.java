package br.com.vrosa.airbrush.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DropHandle {

    @NotNull String itemId();

    int amount();

    @NotNull Vec3 position();

    void consume(int amount);

    /** Ink loaded in the dropped item (quills), or {@code null}. */
    default @Nullable String inkId() {
        return null;
    }

    default @Nullable Integer inkColor() {
        return null;
    }

    /** Remaining durability of the dropped item (the quill's ink charge). */
    default int durabilityRemaining() {
        return 0;
    }
}
