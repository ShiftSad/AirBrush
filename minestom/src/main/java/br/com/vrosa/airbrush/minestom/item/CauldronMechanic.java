package br.com.vrosa.airbrush.minestom.item;

import br.com.vrosa.airbrush.core.ink.CauldronIngredients;
import br.com.vrosa.airbrush.core.AirBrushEngine;
import br.com.vrosa.airbrush.minestom.platform.MinestomPlayer;
import br.com.vrosa.airbrush.minestom.platform.MinestomWorld;
import br.com.vrosa.airbrush.minestom.item.Tags;
import br.com.vrosa.airbrush.platform.ToolType;
import br.com.vrosa.airbrush.platform.Vec3;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CauldronMechanic {

    /** Max time (in ticks) we track a thrown item before giving up on it. */
    private static final int SETTLE_TIMEOUT = 200;

    private static final Map<ItemEntity, Integer> tracked = new ConcurrentHashMap<>();

    private CauldronMechanic() {}

    public static void track(@NotNull ItemEntity drop) {
        if (!isRelevant(drop)) return;
        tracked.put(drop, 0);
    }

    private static boolean isRelevant(@NotNull ItemEntity drop) {
        final var stack = drop.getItemStack();
        final var custom = stack.getTag(Tags.ITEM_TYPE);
        if (custom != null) return CauldronIngredients.accepts(Tags.NAMESPACE + ":" + custom);
        return CauldronIngredients.accepts(stack.material().key().asString());
    }

    public static void tick(@NotNull AirBrushEngine engine) {
        final var iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final var drop = entry.getKey();
            final var instance = drop.getInstance();
            if (drop.isRemoved() || instance == null || entry.getValue() >= SETTLE_TIMEOUT) {
                iterator.remove();
                continue;
            }
            entry.setValue(entry.getValue() + 1);

            final var position = drop.getPosition();
            if (!instance.getBlock(position).compare(Block.WATER_CAULDRON)) continue;
            engine.cauldronService().deposit(MinestomWorld.of(instance),
                    new Vec3(position.blockX(), position.blockY(), position.blockZ()));
            iterator.remove();
        }
    }

    public static boolean dip(@NotNull AirBrushEngine engine, @NotNull PlayerBlockInteractEvent event) {
        if (!event.getBlock().compare(Block.WATER_CAULDRON)) return false;

        final var player = event.getPlayer();
        final var tool = MinestomItems.toolOf(player.getItemInMainHand());
        if (tool != ToolType.MARKER && tool != ToolType.QUILL && tool != ToolType.CLOTH) return false;

        final var world = MinestomWorld.of(event.getInstance());
        final var position = event.getBlockPosition();
        final var block = new Vec3(position.blockX(), position.blockY(), position.blockZ());
        if (!engine.cauldronService().isBrew(world, block)) return false;

        engine.cauldronService().dip(MinestomPlayer.of(player), world, block);
        event.setBlockingItemUse(true);
        return true;
    }
}
