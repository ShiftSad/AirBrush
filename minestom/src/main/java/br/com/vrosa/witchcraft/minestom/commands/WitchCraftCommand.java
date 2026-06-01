package br.com.vrosa.witchcraft.minestom.commands;

import br.com.vrosa.witchcraft.core.i18n.Messages;
import br.com.vrosa.witchcraft.minestom.platform.MinestomPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class WitchCraftCommand extends Command {

    public WitchCraftCommand(@NotNull Runnable reload) {
        super("witchcraft");

        addSyntax((sender, ctx) -> {
            reload.run();
            final var locale = sender instanceof Player player ? MinestomPlayer.of(player).locale() : Locale.US;
            sender.sendMessage(Component.text(Messages.get(locale, Messages.Key.CONFIG_RELOADED), NamedTextColor.GREEN));
        }, ArgumentType.Literal("reload"));
    }
}
