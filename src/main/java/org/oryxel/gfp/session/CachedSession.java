package org.oryxel.gfp.session;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.oryxel.gfp.cache.ChunkCache;
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
    private Vector3d offset = Vector3d.from(0, 0 ,0);

    public Vector3f rotation = Vector3f.ZERO;

    public Vector3f unconfirmedTeleport;

    @Getter
    private final ChunkCache chunkCache = new ChunkCache(this);

    public CachedSession(GeyserSession session) {
        this.session = session;

        GeyserUtil.hookIntoCloudburstMC(this);
    }

    public void kick(String reason) {
        this.session.disconnect(reason);
    }

    public void reOffsetPlayer(double x, double z, Vector3d newOffset) {
        float posX = Float.parseFloat(Double.toString(x)), posZ = Float.parseFloat(Double.toString(z));

        // Silent teleport, also won't suddenly stop player velocity.
        final MoveEntityAbsolutePacket packet = new MoveEntityAbsolutePacket();
        packet.setRuntimeEntityId(this.runtimeId);
        packet.setForceMove(true);
        packet.setTeleported(true);
        packet.setPosition(Vector3f.from(posX, this.session.getPlayerEntity().getPosition().getY(), posZ));
        packet.setRotation(this.rotation);
        this.cloudburstDownstream.sendPacket(packet);

        // Let geyser know about the new position too.
        this.session.getPlayerEntity().setPositionManual(Vector3f.from(posX, this.getSession().getPlayerEntity().getPosition().getY(), posZ));

        Vector3d oldOffset = this.offset.add(0, 0 , 0);
        this.offset = newOffset;

        this.chunkCache.sendChunksWithOffset(oldOffset);

        this.unconfirmedTeleport = packet.getPosition();
    }
}
