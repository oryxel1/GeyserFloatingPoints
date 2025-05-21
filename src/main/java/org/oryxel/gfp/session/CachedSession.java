package org.oryxel.gfp.session;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.cloudburstmc.protocol.bedrock.packet.SetSpawnPositionPacket;
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

    public Vector3f rotation = Vector3f.ZERO;

    public Vector3f unconfirmedTeleport;

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

        // Silent teleport.
        final MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(this.runtimeId);
        packet.setForceMove(true);
        packet.setTeleported(true);
        packet.setPosition(Vector3f.from(posX, this.session.getPlayerEntity().getPosition().getY(), posZ));
        packet.setRotation(this.rotation);
        this.cloudburstDownstream.sendPacket(packet);

        // Let geyser know about the new position too.
        this.session.getPlayerEntity().setPositionManual(Vector3f.from(posX, this.getSession().getPlayerEntity().getPosition().getY(), posZ));
        Vector3i oldOffset = this.offset.add(0, 0 , 0);
        this.offset = newOffset;

        this.chunkCache.sendChunksWithOffset(oldOffset);
        this.entityCache.resendWithOffset();
        this.sendWorldSpawn();

        this.unconfirmedTeleport = packet.getPosition();
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
