package org.oryxel.gfp.protocol.listener;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;

public interface JavaPacketListener extends SessionListener {
    void packetReceived(Session session, MCPLPacketEvent event);
    void packetSent(Session session, MCPLPacketEvent event);

    void packetSending(PacketSendingEvent event);

    void packetError(PacketErrorEvent event);
    void connected(ConnectedEvent event);
    void disconnecting(DisconnectingEvent event);
    void disconnected(DisconnectedEvent event);
}
