package org.oryxel.gfp.util;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.ChunkRadiusUpdatedPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;
import org.oryxel.gfp.session.CachedSession;

public class DimensionUtil {
    public static final int LOADING_SCREEN_ID = 123456987;

    // Slight modifications.
    public static void switchDimension(CachedSession session, int bedrockDimension, boolean real) {
        if (session.getSession().getServerRenderDistance() > 32 && !session.getSession().isEmulatePost1_13Logic()) {
            // The server-sided view distance wasn't a thing until Minecraft Java 1.14
            // So ViaVersion compensates by sending a "view distance" of 64
            // That's fine, except when the actual view distance sent from the server is five chunks
            // The client locks up when switching dimensions, expecting more chunks than it's getting
            // To solve this, we cap at 32 unless we know that the render distance actually exceeds 32
            // Also, as of 1.19: PS4 crashes with a ChunkRadiusUpdatedPacket too large
            session.getSession().getGeyser().getLogger().debug("Applying dimension switching workaround for Bedrock render distance of "
                    + session.getSession().getServerRenderDistance());
            ChunkRadiusUpdatedPacket chunkRadiusUpdatedPacket = new ChunkRadiusUpdatedPacket();
            chunkRadiusUpdatedPacket.setRadius(32);
            session.getSession().sendUpstreamPacket(chunkRadiusUpdatedPacket);
            // Will be re-adjusted on spawn
        }

        ChangeDimensionPacket changeDimensionPacket = new ChangeDimensionPacket();
        changeDimensionPacket.setLoadingScreenId(real ? LOADING_SCREEN_ID : null);
        changeDimensionPacket.setDimension(bedrockDimension);
        changeDimensionPacket.setRespawn(true);
        changeDimensionPacket.setPosition(session.getSession().getPlayerEntity().position());
        session.cloudburstDownstream.sendPacketImmediately(changeDimensionPacket);

        DimensionUtils.setBedrockDimension(session.getSession(), bedrockDimension);

        session.getSession().setLastChunkPosition(null);

        PlayerActionPacket ackPacket = new PlayerActionPacket();
        ackPacket.setRuntimeEntityId(session.runtimeId);
        ackPacket.setAction(PlayerActionType.DIMENSION_CHANGE_SUCCESS);
        ackPacket.setBlockPosition(Vector3i.ZERO);
        ackPacket.setResultPosition(Vector3i.ZERO);
        ackPacket.setFace(0);
        session.getSession().sendUpstreamPacket(ackPacket);

        // TODO - fix this hack of a fix by sending the final dimension switching logic after sections have been sent.
        // The client wants sections sent to it before it can successfully respawn.
        ChunkUtils.sendEmptyChunks(session.getSession(), session.getSession().getPlayerEntity().getPosition().toInt(), 3, true);
    }
}
