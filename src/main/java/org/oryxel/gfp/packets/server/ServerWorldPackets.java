package org.oryxel.gfp.packets.server;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ServerWorldPackets implements JavaPacketListener {
    @Override
    public void packetReceived(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ClientboundSoundPacket packet) {
            event.setPacket(new ClientboundSoundPacket(
                    packet.getSound(), packet.getCategory(),
                    packet.getX() - session.getOffset().getX(),
                    packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getVolume(), packet.getPitch(), packet.getSeed()
            ));
        }

        if (event.getPacket() instanceof ClientboundLevelEventPacket packet) {
            event.setPacket(new ClientboundLevelEventPacket(packet.getEvent(), packet.getPosition().sub(session.getOffset()), packet.getData(), packet.isBroadcast()));
        }

        if (event.getPacket() instanceof ClientboundLevelParticlesPacket packet) {
            event.setPacket(new ClientboundLevelParticlesPacket(packet.getParticle(), packet.isLongDistance(),
                    packet.isAlwaysShow(), packet.getX() - session.getOffset().getX(), packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ(), packet.getVelocityOffset(), packet.getAmount()));
        }

        if (event.getPacket() instanceof ClientboundOpenSignEditorPacket packet) {
            event.setPacket(new ClientboundOpenSignEditorPacket(packet.getPosition().sub(session.getOffset()), packet.isFrontText()));
        }

        if (event.getPacket() instanceof ClientboundPlayerLookAtPacket packet) {
            event.setPacket(new ClientboundPlayerLookAtPacket(packet.getOrigin(), packet.getX() - session.getOffset().getX(), packet.getY(), packet.getZ() - session.getOffset().getZ()));
        }

        if (event.getPacket() instanceof ClientboundSetDefaultSpawnPositionPacket packet) {
            session.worldSpawn = packet.getPosition();
            event.setPacket(new ClientboundSetDefaultSpawnPositionPacket(packet.getPosition().sub(session.getOffset()), packet.getAngle()));
        }
    }
}
