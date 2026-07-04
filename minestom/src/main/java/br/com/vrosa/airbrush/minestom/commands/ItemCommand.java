package br.com.vrosa.airbrush.minestom.commands;

import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.minestom.item.MinestomItems;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import br.com.vrosa.airbrush.platform.Hammer;
import br.com.vrosa.airbrush.platform.Quill;
import br.com.vrosa.airbrush.platform.ToolType;
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

        final var item = ArgumentType.Word("item")
                .from("kit", "pencil", "eraser", "palette", "amethyst_dye", "hammer", "cloth",
                        "quill", "quill_gold", "quill_diamond", "quill_netherite");
        addSyntax((sender, ctx) -> execute(sender, ctx.get(item)), item);
    }

    private static void execute(@NotNull CommandSender sender, @NotNull String id) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return;
        }
        if (player.getPermissionLevel() < 4) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.NO_PERMISSION), NamedTextColor.RED));
            return;
        }

        final var wp = MinestomPlayer.of(player);
        final var normalized = id.toLowerCase(Locale.ROOT);
        final var tool = ToolType.byId(normalized);
        if ("kit".equals(normalized)) {
            wp.giveTool(ToolType.PENCIL);
            wp.giveTool(ToolType.ERASER);
            wp.giveTool(ToolType.PALETTE);
        } else if (Quill.isQuillId(normalized)) {
            player.getInventory().addItemStack(MinestomItems.quill(Quill.tierOf(normalized), 1.0));
        } else if (tool != null) {
            wp.giveTool(tool);
        } else if (Hammer.ID.equals(normalized)) {
            player.getInventory().addItemStack(MinestomItems.hammer());
        } else {
            player.sendMessage(Component.text(Messages.get(wp.locale(), Messages.Key.DRAWITEM_INVALID), NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text(Messages.get(wp.locale(), Messages.Key.DRAWITEM_GIVEN), NamedTextColor.GREEN));
    }
}
