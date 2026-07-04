package br.com.vrosa.airbrush.paper.listeners;

import br.com.vrosa.airbrush.core.ink.CauldronIngredients;
import br.com.vrosa.airbrush.core.ink.CauldronService;
import br.com.vrosa.airbrush.paper.item.ItemFactory;
import br.com.vrosa.airbrush.paper.item.Keys;
import br.com.vrosa.airbrush.paper.platform.BukkitPlayer;
import br.com.vrosa.airbrush.paper.platform.BukkitWorld;
import br.com.vrosa.airbrush.platform.ToolType;
import br.com.vrosa.airbrush.platform.Vec3;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CauldronListener implements Listener {

    /** Max time (in ticks) we track a thrown item before giving up on it. */
    private static final int SETTLE_TIMEOUT = 200;

    private final CauldronService service;
    /** Falling items of interest — each tick we check whether they settled into a WATER_CAULDRON. */
    private final Map<Item, Integer> tracked = new HashMap<>();

    public CauldronListener(@NotNull CauldronService service) {
        this.service = service;
    }

    @EventHandler
    public void onItemSpawn(@NotNull ItemSpawnEvent event) {
        if (!isRelevant(event.getEntity())) return;
        tracked.put(event.getEntity(), 0);
    }

    private static boolean isRelevant(@NotNull Item item) {
        final var stack = item.getItemStack();
        final var meta = stack.getItemMeta();
        if (meta != null) {
            final var custom = meta.getPersistentDataContainer().get(Keys.ITEM_TYPE, PersistentDataType.STRING);
            if (custom != null) return CauldronIngredients.accepts(Keys.NAMESPACE + ":" + custom);
        }
        return CauldronIngredients.accepts(stack.getType().getKey().toString());
    }

    public void clearTracked() {
        tracked.clear();
    }

    public void tick() {
        final var iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final var item = entry.getKey();
            if (!item.isValid() || entry.getValue() >= SETTLE_TIMEOUT) {
                iterator.remove();
                continue;
            }
            entry.setValue(entry.getValue() + 1);

            final var block = item.getLocation().getBlock();
            if (block.getType() != Material.WATER_CAULDRON) continue;
            service.deposit(BukkitWorld.of(block.getWorld()), blockVec(block));
            iterator.remove();
        }
    }

    @EventHandler
    public void onCauldronChange(@NotNull CauldronLevelChangeEvent event) {
        if (service.isBrew(BukkitWorld.of(event.getBlock().getWorld()), blockVec(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    // HIGH so protection plugins (NORMAL) can veto the interaction first;
    // safe with ignoreCancelled because only RIGHT_CLICK_BLOCK is handled.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final var tool = ItemFactory.toolOf(event.getItem());
        if (tool != ToolType.MARKER && tool != ToolType.QUILL && tool != ToolType.CLOTH) return;

        final var block = event.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;

        final var world = BukkitWorld.of(block.getWorld());
        if (!service.isBrew(world, blockVec(block))) return;

        event.setCancelled(true);
        service.dip(BukkitPlayer.of(event.getPlayer()), world, blockVec(block));
    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent event) {
        restore(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent event) {
        final var world = BukkitWorld.of(event.getWorld());
        forEachBrew(event.getChunk(), (position, inkId) -> service.unload(world, position));
    }

    /** Recreates brews persisted in the chunk PDC (called on chunk load and on plugin enable). */
    public void restore(@NotNull Chunk chunk) {
        final var world = BukkitWorld.of(chunk.getWorld());
        forEachBrew(chunk, (position, inkId) -> service.restore(world, position, inkId));
    }

    private static void forEachBrew(@NotNull Chunk chunk, @NotNull BiConsumer<Vec3, String> action) {
        final var pdc = chunk.getPersistentDataContainer();
        for (final var key : pdc.getKeys()) {
            if (!Keys.NAMESPACE.equals(key.getNamespace()) || !key.getKey().startsWith(Keys.BREW_PREFIX)) continue;
            final var inkId = pdc.get(key, PersistentDataType.STRING);
            final var position = parseBrewKey(key.getKey());
            if (inkId != null && position != null) action.accept(position, inkId);
        }
    }

    private static @Nullable Vec3 parseBrewKey(@NotNull String key) {
        final var parts = key.substring(Keys.BREW_PREFIX.length()).split("_");
        if (parts.length != 3) return null;
        try {
            return new Vec3(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final var type = event.getBlock().getType();
        if (type != Material.CAULDRON && type != Material.WATER_CAULDRON) return;
        service.invalidate(BukkitWorld.of(event.getBlock().getWorld()), blockVec(event.getBlock()));
    }

    private static @NotNull Vec3 blockVec(@NotNull Block block) {
        return new Vec3(block.getX(), block.getY(), block.getZ());
    }
}
