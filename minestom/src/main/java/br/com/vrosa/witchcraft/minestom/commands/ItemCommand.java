package br.com.vrosa.witchcraft.minestom.commands;

import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.minestom.platform.MinestomPlayer;
import br.com.vrosa.witchcraft.platform.ToolType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class ItemCommand extends Command {

    public ItemCommand() {
        super("drawitem");

        final var item = ArgumentType.Word("item").from("pencil", "eraser", "palette");
        addSyntax((sender, ctx) -> execute(sender, ctx.get(item)), item);
    }

    private static void execute(@NotNull CommandSender sender, @NotNull String id) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return;
        }

        final var wp = MinestomPlayer.of(player);
        final var tool = ToolType.byId(id.toLowerCase(Locale.ROOT));
        if (tool == null) {
            player.sendMessage(Component.text(Messages.get(wp.locale(), Messages.Key.DRAWITEM_INVALID), NamedTextColor.RED));
            return;
        }

        wp.giveTool(tool);
        player.sendMessage(Component.text(Messages.get(wp.locale(), Messages.Key.DRAWITEM_GIVEN), NamedTextColor.GREEN));
    }
}
