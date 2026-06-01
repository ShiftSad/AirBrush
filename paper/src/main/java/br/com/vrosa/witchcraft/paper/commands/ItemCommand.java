package br.com.vrosa.witchcraft.paper.commands;

import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
import br.com.vrosa.witchcraft.platform.ToolType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ItemCommand {

    private ItemCommand() {}

    public static @NotNull LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("drawitem")
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests(ItemCommand::suggest)
                        .executes(ItemCommand::apply))
                .build();
    }

    private static int apply(@NotNull CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getSender() instanceof Player player)) {
            context.getSource().getSender().sendMessage("Apenas jogadores podem usar /drawitem.");
            return 0;
        }

        final var id = StringArgumentType.getString(context, "item").toLowerCase(Locale.ROOT);
        final var tool = ToolType.byId(id);
        if (tool == null) {
            player.sendMessage("Item inválido. :(");
            return 0;
        }

        BukkitPlayer.of(player).giveTool(tool);
        player.sendMessage("Item adicionado ao inventário.");
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggest(
            @NotNull CommandContext<CommandSourceStack> context, @NotNull SuggestionsBuilder builder) {
        final var input = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (final var tool : ToolType.values()) {
            if (tool.id().startsWith(input)) builder.suggest(tool.id());
        }
        return builder.buildFuture();
    }
}
