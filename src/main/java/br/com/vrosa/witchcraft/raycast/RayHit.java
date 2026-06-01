package br.com.vrosa.witchcraft.raycast;

import org.bukkit.Location;
import org.joml.Vector3f;

public record RayHit(Location position, Vector3f normal) {

    public RayHit copy() {
        return new RayHit(position.clone(), new Vector3f(normal));
    }
}
