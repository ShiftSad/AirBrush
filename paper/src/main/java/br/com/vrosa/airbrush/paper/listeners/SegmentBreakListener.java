package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.paper.platform.BukkitWorld;
import br.com.vrosa.airbrush.platform.Vec3;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/** Strokes are glued to blocks: breaking the block removes the segments drawn on it. */
public final class SegmentBreakListener implements Listener {

    private final AirBrushEngine engine;

    public SegmentBreakListener(@NotNull AirBrushEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final var block = event.getBlock();
        engine.breakAnchored(BukkitWorld.of(block.getWorld()),
                new Vec3(block.getX(), block.getY(), block.getZ()));
    }
}
