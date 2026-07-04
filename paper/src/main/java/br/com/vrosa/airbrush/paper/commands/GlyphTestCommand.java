package br.com.vrosa.airbrush.paper.commands;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.paper.platform.BukkitWorld;
import br.com.vrosa.airbrush.platform.Vec3;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class GlyphTestCommand {

    private static final int PARTICLE_BURSTS = 10;
    private static final long BURST_PERIOD_TICKS = 10;

    private GlyphTestCommand() {}

    public static @NotNull LiteralCommandNode<CommandSourceStack> build(@NotNull AirBrushEngine engine,
                                                                        @NotNull Plugin plugin) {
        return Commands.literal("glyphtest")
                .requires(source -> source.getSender().hasPermission("airbrush.debug"))
                .executes(ctx -> run(ctx, engine, plugin, false))
                .then(Commands.literal("particles").executes(ctx -> run(ctx, engine, plugin, true)))
                .build();
    }

    private static int run(@NotNull CommandContext<CommandSourceStack> ctx, @NotNull AirBrushEngine engine,
                           @NotNull Plugin plugin, boolean particles) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text(
                    Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return 0;
        }

        final var locale = player.locale();
        final var world = new BukkitWorld(player.getWorld());
        final var center = BukkitWorld.toVec3(player.getLocation());
        final var strokes = engine.glyphStrokes().near(world, center, engine.config().glyphAnalysisRadius());
        final var analysis = GlyphAnalyzer.analyze(strokes, new GlyphAnalyzer.Settings(
                engine.config().glyphMinRadius(), engine.config().glyphMaxRadius()));

        if (analysis.glyph().isEmpty()) {
            player.sendMessage(Component.text(Messages.glyphReason(locale, analysis.reason()), NamedTextColor.YELLOW));
            return 0;
        }

        final var glyph = analysis.glyph().get();
        final var breakdown = analysis.breakdown();
        final var modifiers = glyph.modifiers().isEmpty() ? "-" : glyph.modifiers().stream()
                .map(Modifier::name).sorted().collect(Collectors.joining("+"));
        player.sendMessage(Component.text(Messages.format(locale, Messages.Key.GLYPH_TEST_RESULT,
                        glyph.base().name(), modifiers, breakdown.total(), breakdown.closure(),
                        breakdown.regularity(), breakdown.smoothness(), breakdown.cleanliness()),
                NamedTextColor.GREEN));

        if (particles) showParticles(player, plugin, analysis);
        return Command.SINGLE_SUCCESS;
    }

    private static void showParticles(@NotNull Player player, @NotNull Plugin plugin,
                                      @NotNull GlyphAnalysis analysis) {
        final var glyph = analysis.glyph().orElseThrow();
        final var corners = List.copyOf(analysis.corners());
        final var center = glyph.center();
        final var bursts = new AtomicInteger();

        player.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (bursts.getAndIncrement() >= PARTICLE_BURSTS || !player.isOnline()) {
                task.cancel();
                return;
            }
            for (final var corner : corners) {
                dust(player, corner, Color.YELLOW);
            }
            dust(player, center, Color.AQUA);
        }, 0, BURST_PERIOD_TICKS);
    }

    private static void dust(@NotNull Player player, @NotNull Vec3 at, @NotNull Color color) {
        final var location = new Location(player.getWorld(), at.x(), at.y(), at.z());
        player.spawnParticle(Particle.DUST, location, 4, 0.05, 0.05, 0.05,
                new Particle.DustOptions(color, 1.0f));
    }
}
