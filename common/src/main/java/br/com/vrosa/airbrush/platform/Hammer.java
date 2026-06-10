package br.com.vrosa.airbrush.platform;

public final class Hammer {

    public static final String ID = "hammer";
    public static final String NAME_KEY = "item.airbrush.hammer";

    public static final int DURABILITY = 150;
    public static final double ATTACK_DAMAGE = 9.0;
    public static final double ATTACK_SPEED = 0.8;
    public static final float MINING_SPEED = 4.0f;

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    public static final double ATTACK_DAMAGE_MODIFIER = ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE;
    public static final double ATTACK_SPEED_MODIFIER = ATTACK_SPEED - PLAYER_BASE_ATTACK_SPEED;

    private Hammer() {}
}
