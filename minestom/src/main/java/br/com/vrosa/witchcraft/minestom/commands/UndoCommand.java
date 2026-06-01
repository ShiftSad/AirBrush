package br.com.vrosa.witchcraft.minestom.commands;

import br.com.vrosa.witchcraft.core.history.History;
import br.com.vrosa.witchcraft.minestom.platform.MinestomPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class UndoCommand extends Command {

    public UndoCommand(@NotNull History history) {
        super("undo");

        final var amount = ArgumentType.Integer("quantidade").min(1);
        setDefaultExecutor((sender, ctx) -> execute(sender, history, 1));
        addSyntax((sender, ctx) -> execute(sender, history, ctx.get(amount)), amount);
    }

    private static void execute(@NotNull CommandSender sender, @NotNull History history, int count) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Apenas jogadores podem usar /undo.", NamedTextColor.RED));
            return;
        }

        final var result = history.undo(MinestomPlayer.of(player), count);
        if (result.changes() == 0) {
            player.sendMessage(Component.text("Nada para desfazer.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text(
                "Desfeitas " + result.changes() + " alteração(ões) (" + result.segments() + " segmento(s)).",
                NamedTextColor.GREEN));
    }
}
