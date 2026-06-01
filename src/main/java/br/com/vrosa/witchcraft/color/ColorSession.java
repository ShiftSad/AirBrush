package br.com.vrosa.witchcraft.color;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ColorSession {

    public void build(Player player) {
        final var location = player.getLocation();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                spawnDisplay(location.clone().add(i * 0.25, 0.5, j * 0.25));
            }
        }
    }

    public void spawnDisplay(Location location) {
        final var world = location.getWorld();
        if (world == null) return;

        // /summon minecraft:text_display ~ ~1 ~ {text:'" "',background:-65536,text_opacity:0,see_through:0b,billboard:"center",transformation:{scale:[0.75f,1f,1f],translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}
        world.spawn(location, TextDisplay.class, display -> {
            display.text(Component.text("\" \""));
            display.setBackgroundColor(Color.RED);
            display.setTextOpacity((byte) 0);
            display.setSeeThrough(false);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(new Vector3f(0.75f, 1f, 1f), new Quaternionf(), new Vector3f(0f, 0f, 0f), new Quaternionf()));
        });
    }
}
