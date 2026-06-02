package br.com.vrosa.airbrush.paper.commands;

import br.com.vrosa.airbrush.core.i18n.Messages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class AirBrushCommand {

    private AirBrushCommand() {}

    public static @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull Runnable reload) {
        return Commands.literal("airbrush")
                .requires(source -> source.getSender().hasPermission("airbrush.reload"))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx, reload)))
                .build();
    }

    private static int reload(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull Runnable reload) {
        reload.run();
        final var sender = ctx.getSource().getSender();
        final var locale = sender instanceof Player player ? player.locale() : Locale.US;
        sender.sendMessage(Component.text(Messages.get(locale, Messages.Key.CONFIG_RELOADED), NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
