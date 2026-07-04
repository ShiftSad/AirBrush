package br.com.vrosa.airbrush.paper.item;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public final class Keys {

    public static final String NAMESPACE = "airbrush";

    public static final NamespacedKey ITEM_TYPE = new NamespacedKey(NAMESPACE, "item_type");
    public static final NamespacedKey TIER = new NamespacedKey(NAMESPACE, "tier");
    public static final NamespacedKey QUALITY = new NamespacedKey(NAMESPACE, "quality");
    public static final NamespacedKey HAMMER_RECIPE = new NamespacedKey(NAMESPACE, "hammer");
    public static final NamespacedKey HAMMER_MODEL = new NamespacedKey(NAMESPACE, "hammer");
    public static final NamespacedKey RADIUS = new NamespacedKey(NAMESPACE, "radius");
    public static final NamespacedKey INK = new NamespacedKey(NAMESPACE, "ink");
    public static final NamespacedKey INK_COLOR = new NamespacedKey(NAMESPACE, "ink_color");
    public static final NamespacedKey SOLVENT = new NamespacedKey(NAMESPACE, "solvent");
    public static final NamespacedKey STROKE_ID = new NamespacedKey(NAMESPACE, "stroke_id");
    public static final NamespacedKey SEGMENT_ID = new NamespacedKey(NAMESPACE, "segment_id");
    public static final NamespacedKey SEGMENT_COLOR = new NamespacedKey(NAMESPACE, "segment_color");
    public static final NamespacedKey SEGMENT_ANCHOR = new NamespacedKey(NAMESPACE, "segment_anchor");

    public static final Key SEGMENT_MODEL = Key.key(NAMESPACE, "drawing_segment");
    public static final Key SELECTION_MODEL = Key.key(NAMESPACE, "selection");

    /** Prefix for brew keys in the chunk PDC: {@code brew_<x>_<y>_<z>} (global coords). */
    public static final String BREW_PREFIX = "brew_";

    public static NamespacedKey brew(int x, int y, int z) {
        return new NamespacedKey(NAMESPACE, BREW_PREFIX + x + "_" + y + "_" + z);
    }

    private Keys() {}
}
