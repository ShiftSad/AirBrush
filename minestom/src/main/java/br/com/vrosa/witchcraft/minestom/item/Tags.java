package br.com.vrosa.witchcraft.minestom.item;

import net.minestom.server.tag.Tag;

public final class Tags {

    public static final String NAMESPACE = "witchcraft";

    public static final Tag<String> ITEM_TYPE = Tag.String("witchcraft:item_type");
    public static final Tag<String> STROKE_ID = Tag.String("witchcraft:stroke_id");
    public static final Tag<String> SEGMENT_ID = Tag.String("witchcraft:segment_id");
    public static final Tag<Integer> SEGMENT_COLOR = Tag.Integer("witchcraft:segment_color");

    public static final String SEGMENT_MODEL = NAMESPACE + ":drawing_segment";
    public static final String SELECTION_MODEL = NAMESPACE + ":selection";

    private Tags() {}
}
