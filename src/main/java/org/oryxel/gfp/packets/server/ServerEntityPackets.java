package org.oryxel.gfp.packets.server;

import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ServerEntityPackets implements JavaPacketListener {
    @Override
    public void packetReceived(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            session.getEntityCache().cacheEntity(packet.getEntityId(), Vector3d.from(packet.getX(), packet.getY(), packet.getZ()));

            event.setPacket(new ClientboundAddEntityPacket(
                    packet.getEntityId(), packet.getUuid(), packet.getType(), packet.getData(),
                    packet.getX() - session.getOffset().getX(),
                    packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getYaw(), packet.getHeadYaw(), packet.getPitch(),
                    packet.getMotionX(), packet.getMotionY(), packet.getMotionZ()
            ));
        }

        if (event.getPacket() instanceof ClientboundRemoveEntitiesPacket packet) {
            for (int id : packet.getEntityIds()) {
                session.getEntityCache().remove(id);
            }
        }

        if (event.getPacket() instanceof ClientboundEntityPositionSyncPacket packet) {
            session.getEntityCache().moveAbsolute(packet.getId(), packet.getPosition());

            event.setPacket(new ClientboundEntityPositionSyncPacket(
                    packet.getId(), packet.getPosition().sub(session.getOffset().toDouble()),
                    packet.getDeltaMovement(), packet.getYRot(), packet.getXRot(), packet.isOnGround()
            ));
        }

        if (event.getPacket() instanceof ClientboundMoveEntityPosRotPacket packet) {
            session.getEntityCache().moveRelative(packet.getEntityId(), Vector3d.from(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ()));
        }

        if (event.getPacket() instanceof ClientboundMoveEntityPosPacket packet) {
            session.getEntityCache().moveRelative(packet.getEntityId(), Vector3d.from(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ()));
        }

        if (event.getPacket() instanceof ClientboundMoveVehiclePacket packet) {
            event.setPacket(new ClientboundMoveVehiclePacket(packet.getPosition().sub(session.getOffset().toDouble()),
                    packet.getYRot(), packet.getXRot()));
        }
    }
}
