package org.oryxel.gfp.packets;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityPositionSyncPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveVehiclePacket;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ServerEntityPackets implements JavaPacketListener {
    @Override
    public void packetReceived(Session javaSession, MCPLPacketEvent event) {
        final CachedSession session = event.getPlayer();

        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            event.setPacket(new ClientboundAddEntityPacket(
                    packet.getEntityId(), packet.getUuid(), packet.getType(), packet.getData(),
                    packet.getX() - session.getOffset().getX(),
                    packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getYaw(), packet.getHeadYaw(), packet.getPitch(),
                    packet.getMotionX(), packet.getMotionY(), packet.getMotionZ()
            ));
        }

        if (event.getPacket() instanceof ClientboundEntityPositionSyncPacket packet) {
            event.setPacket(new ClientboundEntityPositionSyncPacket(
                    packet.getId(), packet.getPosition().sub(session.getOffset().toDouble()),
                    packet.getDeltaMovement(), packet.getYRot(), packet.getXRot(), packet.isOnGround()
            ));
        }

        if (event.getPacket() instanceof ClientboundMoveVehiclePacket packet) {
            event.setPacket(new ClientboundMoveVehiclePacket(packet.getPosition().sub(session.getOffset().toDouble()),
                    packet.getYRot(), packet.getXRot()));
        }
    }
}
