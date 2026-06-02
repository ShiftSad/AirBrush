package br.com.vrosa.airbrush.paper.commands;

import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import br.com.vrosa.airbrush.platform.ToolType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            context.getSource().getSender().sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return 0;
        }

        final var locale = player.locale();
        final var tool = ToolType.byId(StringArgumentType.getString(context, "item").toLowerCase(Locale.ROOT));
        if (tool == null) {
            player.sendMessage(Component.text(Messages.get(locale, Messages.Key.DRAWITEM_INVALID), NamedTextColor.RED));
            return 0;
        }

        BukkitPlayer.of(player).giveTool(tool);
        player.sendMessage(Component.text(Messages.get(locale, Messages.Key.DRAWITEM_GIVEN), NamedTextColor.GREEN));
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
