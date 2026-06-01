package br.com.vrosa.witchcraft.commands;

import br.com.vrosa.witchcraft.history.History;
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
            ctx.getSource().getSender().sendMessage(Component.text("Apenas jogadores podem usar /undo.", NamedTextColor.RED));
            return 0;
        }

        final var result = history.undo(player, count);
        if (result.changes() == 0) {
            player.sendMessage(Component.text("Nada para desfazer.", NamedTextColor.YELLOW));
            return 0;
        }

        player.sendMessage(Component.text(
                "Desfeitas " + result.changes() + " alteração(ões) (" + result.segments() + " segmento(s)).",
                NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
