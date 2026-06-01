package br.com.vrosa.witchcraft.draw;

import br.com.vrosa.witchcraft.raycast.RayHit;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DrawSession {

    final DrawMode mode;
    final int rgb;
    final UUID strokeId = UUID.randomUUID();
    final List<RayHit> samples = new ArrayList<>();
    final List<ItemDisplay> preview = new ArrayList<>();
    final List<ItemDisplay> committed = new ArrayList<>();
    RayHit anchor;
    ItemDisplay rubberband;

    DrawSession(DrawMode mode, int rgb) {
        this.mode = mode;
        this.rgb = rgb;
    }
}
