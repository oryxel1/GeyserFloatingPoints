package org.oryxel.gfp.session;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.SetSpawnPositionPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.oryxel.gfp.cache.ChunkCache;
import org.oryxel.gfp.cache.EntityCache;
import org.oryxel.gfp.geyser.util.GeyserUtil;
import org.oryxel.gfp.protocol.mitm.CloudburstReceiveListener;
import org.oryxel.gfp.protocol.mitm.CloudburstSendListener;

public class CachedSession {
    @Getter
    private final GeyserSession session;

    public ClientSession mcplSession;
    public BedrockServerSession cloudburstDownstream;
    public CloudburstSendListener cloudburstUpstream;
    public CloudburstReceiveListener downstreamPacketHandler;

    public long runtimeId = -1;

    @Getter
    @Setter
    private Vector3i offset = Vector3i.from(0, 0 ,0);

    public Vector3i lastRealPosInt = Vector3i.ZERO;
    public Vector3f rotation = Vector3f.ZERO;
    public Vector3f cachedVelocity = Vector3f.ZERO;

    public boolean silentDimensionSwitch = false;

    @Getter
    private final ChunkCache chunkCache = new ChunkCache(this);
    @Getter
    private final EntityCache entityCache = new EntityCache(this);

    public Vector3i worldSpawn = null;

    public CachedSession(GeyserSession session) {
        this.session = session;

        GeyserUtil.hookIntoCloudburstMC(this);
    }

    public void kick(String reason) {
        this.session.disconnect(reason);
    }

    public void reOffsetPlayer(double x, double z, Vector3i newOffset) {
        float posX = Float.parseFloat(Double.toString(x)), posZ = Float.parseFloat(Double.toString(z));

        this.session.getPlayerEntity().setPositionManual(Vector3f.from(posX, this.getSession().getPlayerEntity().getPosition().getY(), posZ));

        this.offset = newOffset;

        this.chunkCache.sendChunksWithOffset();
        this.entityCache.resendWithOffset();
        this.sendWorldSpawn();
    }

    public void sendWorldSpawn() {
        if (this.worldSpawn == null) {
            return;
        }

        SetSpawnPositionPacket spawnPositionPacket = new SetSpawnPositionPacket();
        spawnPositionPacket.setBlockPosition(worldSpawn.sub(this.offset));
        spawnPositionPacket.setDimensionId(DimensionUtils.javaToBedrock(this.session));
        spawnPositionPacket.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);
        this.session.sendUpstreamPacket(spawnPositionPacket);
    }
}
