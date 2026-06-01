package br.com.vrosa.witchcraft.paper.commands;

import br.com.vrosa.witchcraft.core.draw.DrawService;
import br.com.vrosa.witchcraft.paper.platform.BukkitPlayer;
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ColorCommand {

    private ColorCommand() {}

    public static @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull DrawService service) {
        return Commands.literal("color")
                .then(Commands.argument("cor", StringArgumentType.word())
                        .suggests(ColorCommand::suggest)
                        .executes(ctx -> apply(ctx, service)))
                .build();
    }

    private static int apply(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull DrawService service) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Apenas jogadores podem usar /color.", NamedTextColor.RED));
            return 0;
        }

        final var rgb = parse(StringArgumentType.getString(ctx, "cor"));
        if (rgb == null) {
            player.sendMessage(Component.text("Cor inválida. Use #RRGGBB ou um nome (red, blue, green, ...).", NamedTextColor.RED));
            return 0;
        }

        service.setColor(BukkitPlayer.of(player), rgb);
        player.sendMessage(Component.text("Cor do lápis alterada.", TextColor.color(rgb)));
        return Command.SINGLE_SUCCESS;
    }

    private static @Nullable Integer parse(@NotNull String raw) {
        final var s = raw.trim();
        if (s.isEmpty()) return null;

        final var hex = s.startsWith("#") ? s.substring(1) : s;
        if (hex.matches("(?i)[0-9a-f]{6}")) {
            return Integer.parseInt(hex, 16);
        }

        final var named = NamedTextColor.NAMES.value(s.toLowerCase(Locale.ROOT));
        return named == null ? null : named.value();
    }

    private static CompletableFuture<Suggestions> suggest(
            @NotNull CommandContext<CommandSourceStack> ctx, @NotNull SuggestionsBuilder builder) {
        final var input = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (final var name : NamedTextColor.NAMES.keys()) {
            if (name.startsWith(input)) builder.suggest(name);
        }
        return builder.buildFuture();
    }
}
