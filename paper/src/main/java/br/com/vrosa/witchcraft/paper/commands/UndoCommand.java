package br.com.vrosa.witchcraft.paper.commands;

import br.com.vrosa.witchcraft.core.history.History;
import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class UndoCommand {

    private UndoCommand() {}

    public static @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull History history) {
        return Commands.literal("undo")
                .executes(ctx -> apply(ctx, history, 1))
                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1))
                        .executes(ctx -> apply(ctx, history, IntegerArgumentType.getInteger(ctx, "quantidade"))))
                .build();
    }

    private static int apply(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull History history, int count) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return 0;
        }

        final var locale = player.locale();
        final var result = history.undo(BukkitPlayer.of(player), count);
        if (result.changes() == 0) {
            player.sendMessage(Component.text(Messages.get(locale, Messages.Key.UNDO_NOTHING), NamedTextColor.YELLOW));
            return 0;
        }

        player.sendMessage(Component.text(
                Messages.format(locale, Messages.Key.UNDO_DONE, result.changes(), result.segments()),
                NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
