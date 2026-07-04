package br.com.vrosa.airbrush.minestom.commands;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.core.glyph.geometry.GlyphAnalyzer;
import br.com.vrosa.airbrush.core.glyph.model.GlyphAnalysis;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import br.com.vrosa.airbrush.core.i18n.Messages;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import br.com.vrosa.airbrush.minestom.platform.MinestomWorld;
import br.com.vrosa.airbrush.platform.Vec3;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class GlyphTestCommand extends Command {

    private static final int PARTICLE_BURSTS = 10;
    private static final int BURST_PERIOD_TICKS = 10;
    private static final Color CORNER_COLOR = new Color(0xFFFF55);
    private static final Color CENTER_COLOR = new Color(0x55FFFF);

    public GlyphTestCommand(@NotNull AirBrushEngine engine) {
        super("glyphtest");

        setDefaultExecutor((sender, ctx) -> run(sender, engine, false));
        addSyntax((sender, ctx) -> run(sender, engine, true), ArgumentType.Literal("particles"));
    }

    private static void run(@NotNull CommandSender sender, @NotNull AirBrushEngine engine, boolean particles) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(Messages.get(Locale.US, Messages.Key.PLAYERS_ONLY), NamedTextColor.RED));
            return;
        }

        final var wp = MinestomPlayer.of(player);
        final var locale = wp.locale();
        final var world = new MinestomWorld(player.getInstance());
        final var position = player.getPosition();
        final var center = new Vec3(position.x(), position.y(), position.z());
        final var strokes = engine.glyphStrokes().near(world, center, engine.config().glyphAnalysisRadius());
        final var analysis = GlyphAnalyzer.analyze(strokes, new GlyphAnalyzer.Settings(
                engine.config().glyphMinRadius(), engine.config().glyphMaxRadius()));

        if (analysis.glyph().isEmpty()) {
            player.sendMessage(Component.text(Messages.glyphReason(locale, analysis.reason()), NamedTextColor.YELLOW));
            return;
        }

        final var glyph = analysis.glyph().get();
        final var breakdown = analysis.breakdown();
        final var modifiers = glyph.modifiers().isEmpty() ? "-" : glyph.modifiers().stream()
                .map(Modifier::name).sorted().collect(Collectors.joining("+"));
        player.sendMessage(Component.text(Messages.format(locale, Messages.Key.GLYPH_TEST_RESULT,
                        glyph.base().name(), modifiers, breakdown.total(), breakdown.closure(),
                        breakdown.regularity(), breakdown.smoothness(), breakdown.cleanliness()),
                NamedTextColor.GREEN));

        if (particles) showParticles(player, analysis);
    }

    private static void showParticles(@NotNull Player player, @NotNull GlyphAnalysis analysis) {
        final var glyph = analysis.glyph().orElseThrow();
        final var corners = List.copyOf(analysis.corners());
        final var center = glyph.center();
        final var bursts = new AtomicInteger();

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            if (bursts.getAndIncrement() >= PARTICLE_BURSTS || !player.isOnline()) return TaskSchedule.stop();
            for (final var corner : corners) {
                dust(player, corner, CORNER_COLOR);
            }
            dust(player, center, CENTER_COLOR);
            return TaskSchedule.tick(BURST_PERIOD_TICKS);
        });
    }

    private static void dust(@NotNull Player player, @NotNull Vec3 at, @NotNull Color color) {
        player.sendPacket(new ParticlePacket(Particle.DUST.withColor(color).withScale(1.0f),
                new Vec(at.x(), at.y(), at.z()), new Vec(0.05, 0.05, 0.05), 0f, 4));
    }
}
