package br.com.vrosa.airbrush.minestom.item;

import net.minestom.server.tag.Tag;

public final class Tags {

    public static final String NAMESPACE = "airbrush";

    public static final Tag<String> ITEM_TYPE = Tag.String("airbrush:item_type");
    public static final Tag<Integer> TIER = Tag.Integer("airbrush:tier");
    public static final Tag<Double> QUALITY = Tag.Double("airbrush:quality");
    public static final Tag<Double> RADIUS = Tag.Double("airbrush:radius");
    public static final Tag<String> INK = Tag.String("airbrush:ink");
    public static final Tag<Integer> INK_COLOR = Tag.Integer("airbrush:ink_color");
    public static final Tag<String> SOLVENT = Tag.String("airbrush:solvent");
    public static final Tag<String> STROKE_ID = Tag.String("airbrush:stroke_id");
    public static final Tag<String> SEGMENT_ID = Tag.String("airbrush:segment_id");
    public static final Tag<Integer> SEGMENT_COLOR = Tag.Integer("airbrush:segment_color");
    public static final Tag<Boolean> SEGMENT_BRIGHT = Tag.Boolean("airbrush:segment_bright");
    /** Block the segment is drawn on, encoded as {@code x_y_z} (block coords). */
    public static final Tag<String> SEGMENT_ANCHOR = Tag.String("airbrush:segment_anchor");

    public static final String SEGMENT_MODEL = NAMESPACE + ":drawing_segment";
    public static final String SELECTION_MODEL = NAMESPACE + ":selection";

    private Tags() {}
}
