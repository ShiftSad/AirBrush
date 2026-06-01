package br.com.vrosa.witchcraft.keys;

public enum WitchCraftKey {

    WITCHCRAFT_KEY("witchcraft"),
    ITEM_TYPE("item_type"),
    STROKE_ID("stroke_id"),
    SEGMENT_ID("segment_id"),
    SEGMENT_COLOR("segment_color");

    private final String key;

    WitchCraftKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
