package br.com.vrosa.witchcraft.keys;

public enum WitchCraftKey {

    WITCHCRAFT_KEY("witchcraft"),
    ITEM_TYPE("item_type");

    private final String key;

    WitchCraftKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
