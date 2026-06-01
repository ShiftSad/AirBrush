package br.com.vrosa.witchCraft.draw;

import br.com.vrosa.witchCraft.raycast.RayHit;
import org.bukkit.entity.BlockDisplay;

import java.util.ArrayList;
import java.util.List;

final class DrawSession {

    final DrawMode mode;
    final List<RayHit> samples = new ArrayList<>();
    final List<BlockDisplay> preview = new ArrayList<>();
    RayHit anchor;
    BlockDisplay rubberband;

    DrawSession(DrawMode mode) {
        this.mode = mode;
    }
}
