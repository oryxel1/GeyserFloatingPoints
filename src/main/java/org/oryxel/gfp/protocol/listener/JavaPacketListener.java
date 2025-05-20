package org.oryxel.gfp.protocol.listener;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;

public interface JavaPacketListener {
    default void packetReceived(Session session, MCPLPacketEvent event) {

    }

    default void packetSent(Session session, Packet packet) {

    }

    default void packetSending(MCPLPacketEvent event) {

    }

    default void packetError(PacketErrorEvent event) {

    }

    default void connected(ConnectedEvent event) {

    }

    default void disconnecting(DisconnectingEvent event) {

    }

    default void disconnected(DisconnectedEvent event) {

    }
}
