package org.oryxel.gfp.packets.server;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
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
    }
}
