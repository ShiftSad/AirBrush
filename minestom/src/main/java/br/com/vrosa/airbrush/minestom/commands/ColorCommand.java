package br.com.vrosa.airbrush.minestom.commands;

import br.com.vrosa.airbrush.core.draw.DrawService;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ColorCommand extends Command {

    public ColorCommand(@NotNull DrawService service) {
        super("color");

        final var cor = ArgumentType.Word("cor");
        addSyntax((sender, ctx) -> execute(sender, service, ctx.get(cor)), cor);
    }

    private static void execute(@NotNull CommandSender sender, @NotNull DrawService service, @NotNull String raw) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return;
        }

        final var wp = MinestomPlayer.of(player);
        final var locale = wp.locale();
        final var rgb = parse(raw);
        if (rgb == null) {
            player.sendMessage(Component.text(Messages.get(locale, Messages.Key.COLOR_INVALID), NamedTextColor.RED));
            return;
        }

        service.setColor(wp, rgb);
        player.sendMessage(Component.text(Messages.get(locale, Messages.Key.COLOR_CHANGED), TextColor.color(rgb)));
    }

    private static @Nullable Integer parse(@NotNull String raw) {
        final var s = raw.trim();
        if (s.isEmpty()) return null;

        final var hex = s.startsWith("#") ? s.substring(1) : s;
        if (hex.matches("(?i)[0-9a-f]{6}")) {
            return Integer.parseInt(hex, 16);
        }

        final var named = NamedTextColor.NAMES.value(s.toLowerCase(Locale.ROOT));
        return named == null ? null : named.value();
    }
}
