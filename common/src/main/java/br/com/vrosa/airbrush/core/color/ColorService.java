package br.com.vrosa.airbrush.core.color;

import br.com.vrosa.airbrush.core.draw.DrawService;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.platform.Platform;
import br.com.vrosa.airbrush.platform.Sounds;
import br.com.vrosa.airbrush.platform.WPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ColorService {

    private static final long REOPEN_COOLDOWN_MS = 500;

    private final Platform platform;
    private final DrawService drawService;
    private final Map<UUID, ColorPicker> pickers = new HashMap<>();
    private final Map<UUID, Long> closedAt = new HashMap<>();

    public ColorService(@NotNull Platform platform, @NotNull DrawService drawService) {
        this.platform = platform;
        this.drawService = drawService;
    }

    public boolean isOpen(@NotNull WPlayer player) {
        return pickers.containsKey(player.uuid());
    }

    public void rightClick(@NotNull WPlayer player) {
        final var picker = pickers.get(player.uuid());
        if (picker == null) {
            final var closed = closedAt.get(player.uuid());
            if (closed == null || System.currentTimeMillis() - closed >= REOPEN_COOLDOWN_MS) open(player);
            return;
        }

        final var chosen = picker.click();
        if (chosen == null) {
            player.playSound(Sounds.UI_BUTTON_CLICK, 0.6f, 1.4f);
            return;
        }

        final int rgb = chosen & 0xFFFFFF;
        drawService.setColor(player, rgb);
        close(player);
        player.playSound(Sounds.PLAYER_LEVELUP, 0.6f, 1.8f);
        player.actionBar(Component.text(Messages.get(player.locale(), Messages.Key.COLOR_CHOSEN), TextColor.color(rgb)));
    }

    public void close(@NotNull WPlayer player) {
        final var picker = pickers.remove(player.uuid());
        if (picker != null) {
            picker.despawn();
            closedAt.put(player.uuid(), System.currentTimeMillis());
        }
    }

    public void tick(@NotNull WPlayer player) {
        final var picker = pickers.get(player.uuid());
        if (picker == null) return;

        if (picker.shouldClose()) {
            close(player);
            player.actionBar(Component.text(Messages.get(player.locale(), Messages.Key.PALETTE_CLOSED), NamedTextColor.GRAY));
            return;
        }
        picker.tick();
    }

    private void open(@NotNull WPlayer player) {
        final var hsv = Hsv.toHsv(drawService.colorOf(player));
        final var picker = new ColorPicker(platform, player, hsv[0], hsv[1], hsv[2]);
        picker.spawn();
        pickers.put(player.uuid(), picker);
        player.playSound(Sounds.NOTE_BLOCK_HAT, 0.7f, 1.5f);
        player.actionBar(Component.text(Messages.get(player.locale(), Messages.Key.PALETTE_HINT), NamedTextColor.GRAY));
    }
}
