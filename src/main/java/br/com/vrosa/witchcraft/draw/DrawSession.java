package br.com.vrosa.witchcraft.draw;

import br.com.vrosa.witchcraft.raycast.RayHit;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.List;

final class DrawSession {

    final DrawMode mode;
    final int rgb;
    final List<RayHit> samples = new ArrayList<>();
    final List<ItemDisplay> preview = new ArrayList<>();
    RayHit anchor;
    ItemDisplay rubberband;

    DrawSession(DrawMode mode, int rgb) {
        this.mode = mode;
        this.rgb = rgb;
    }
}
