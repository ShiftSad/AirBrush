package br.com.vrosa.witchcraft.color;

import br.com.vrosa.witchcraft.draw.DrawService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ColorService {

    private final DrawService drawService;
    private final Map<UUID, ColorPicker> pickers = new HashMap<>();

    public ColorService(@NotNull DrawService drawService) {
        this.drawService = drawService;
    }

    public boolean isOpen(@NotNull Player player) {
        return pickers.containsKey(player.getUniqueId());
    }

    public void rightClick(@NotNull Player player) {
        final var picker = pickers.get(player.getUniqueId());
        if (picker == null) {
            open(player);
            return;
        }

        final var chosen = picker.click();
        if (chosen == null) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            return;
        }

        final int rgb = chosen.asRGB();
        drawService.setColor(player, rgb);
        close(player);
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
        player.sendActionBar(Component.text("Cor escolhida.", TextColor.color(rgb)));
    }

    public void close(@NotNull Player player) {
        final var picker = pickers.remove(player.getUniqueId());
        if (picker != null) picker.despawn();
    }

    public void tick(@NotNull Player player) {
        final var picker = pickers.get(player.getUniqueId());
        if (picker == null) return;

        if (picker.shouldClose()) {
            close(player);
            player.sendActionBar(Component.text("Paleta fechada.", NamedTextColor.GRAY));
            return;
        }
        picker.tick();
    }

    private void open(@NotNull Player player) {
        final var hsv = Hsv.toHsv(org.bukkit.Color.fromRGB(drawService.colorOf(player)));
        final var picker = new ColorPicker(player, hsv[0], hsv[1], hsv[2]);
        picker.spawn();
        pickers.put(player.getUniqueId(), picker);
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.5f);
        player.sendActionBar(Component.text("Clique para mover o seletor; clique de novo para confirmar.", NamedTextColor.GRAY));
    }
}
