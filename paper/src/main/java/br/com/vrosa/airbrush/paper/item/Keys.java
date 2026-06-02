package br.com.vrosa.airbrush.paper.item;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;

public final class Keys {

    public static final String NAMESPACE = "airbrush";

    public static final NamespacedKey ITEM_TYPE = new NamespacedKey(NAMESPACE, "item_type");
    public static final NamespacedKey STROKE_ID = new NamespacedKey(NAMESPACE, "stroke_id");
    public static final NamespacedKey SEGMENT_ID = new NamespacedKey(NAMESPACE, "segment_id");
    public static final NamespacedKey SEGMENT_COLOR = new NamespacedKey(NAMESPACE, "segment_color");

    public static final Key SEGMENT_MODEL = Key.key(NAMESPACE, "drawing_segment");
    public static final Key SELECTION_MODEL = Key.key(NAMESPACE, "selection");

    private Keys() {}
}
