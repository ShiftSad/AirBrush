package br.com.vrosa.airbrush.core.draw;

import br.com.vrosa.airbrush.platform.Pose;
import br.com.vrosa.airbrush.platform.SegmentHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DrawSession {

    final DrawMode mode;
    final int rgb;
    final float width;
    final UUID strokeId = UUID.randomUUID();
    final List<Pose> samples = new ArrayList<>();
    final List<SegmentHandle> preview = new ArrayList<>();
    final List<SegmentHandle> committed = new ArrayList<>();
    Pose anchor;
    SegmentHandle rubberband;

    DrawSession(DrawMode mode, int rgb, float width) {
        this.mode = mode;
        this.rgb = rgb;
        this.width = width;
    }
}
