package org.oryxel.gfp.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.geyser.entity.type.Entity;
import org.oryxel.gfp.session.CachedSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class EntityCache {
    @Getter
    private final CachedSession session;

    private final Map<Integer, Vector3d> entities = new ConcurrentHashMap<>();

    public void resendWithOffset() {
        for (Map.Entry<Integer, Vector3d> entry : this.entities.entrySet()) {
            Entity geyserEntity = this.session.getSession().getEntityCache().getEntityByJavaId(entry.getKey());
            if (geyserEntity == null) {
                continue;
            }

            // Since we switched dimension, we have to respawn entity!
            geyserEntity.setPosition(entry.getValue().sub(this.session.getOffset().toDouble()).toFloat());
            geyserEntity.spawnEntity();
        }
    }

    public void cacheEntity(int entityId, Vector3d position) {
        this.entities.put(entityId, position);
    }

    public void moveRelative(int entityId, Vector3d delta) {
        this.moveAbsolute(entityId, this.entities.getOrDefault(entityId, Vector3d.ZERO).add(delta));
    }

    public void moveAbsolute(int entityId, Vector3d position) {
        if (!this.entities.containsKey(entityId)) {
            return;
        }

        this.entities.put(entityId, position);
    }

    public void remove(int entityId) {
        this.entities.remove(entityId);
    }
}
