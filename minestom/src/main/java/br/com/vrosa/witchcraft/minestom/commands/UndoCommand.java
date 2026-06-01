package br.com.vrosa.witchcraft.minestom.commands;

import br.com.vrosa.witchcraft.core.history.History;
import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.minestom.platform.MinestomPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class UndoCommand extends Command {

    public UndoCommand(@NotNull History history) {
        super("undo");

        final var amount = ArgumentType.Integer("quantidade").min(1);
        setDefaultExecutor((sender, ctx) -> execute(sender, history, 1));
        addSyntax((sender, ctx) -> execute(sender, history, ctx.get(amount)), amount);
    }

    private static void execute(@NotNull CommandSender sender, @NotNull History history, int count) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return;
        }

        final var wp = MinestomPlayer.of(player);
        final var locale = wp.locale();
        final var result = history.undo(wp, count);
        if (result.changes() == 0) {
            player.sendMessage(Component.text(Messages.get(locale, Messages.Key.UNDO_NOTHING), NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text(
                Messages.format(locale, Messages.Key.UNDO_DONE, result.changes(), result.segments()),
                NamedTextColor.GREEN));
    }
}
