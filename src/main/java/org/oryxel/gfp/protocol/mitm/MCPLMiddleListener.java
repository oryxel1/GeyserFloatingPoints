package org.oryxel.gfp.protocol.mitm;

import lombok.RequiredArgsConstructor;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.protocol.PacketEvents;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;

import java.util.List;

@RequiredArgsConstructor
public class MCPLMiddleListener extends SessionAdapter {
    private final CachedSession player;
    private final List<SessionListener> listeners;

    @Override
    public void packetReceived(Session session, Packet packet) {
        // if (session != player.getTcpSession()) return;

        final MCPLPacketEvent event = new MCPLPacketEvent(this.player, packet);
        for (final JavaPacketListener listener : PacketEvents.getApi().getJavaListeners()) {
            listener.packetReceived(event);
        }
        if (!event.isCancelled()) {
            listeners.forEach(l -> l.packetReceived(session, event.getPacket()));
        }

        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        final MCPLPacketEvent mcplEvent = new MCPLPacketEvent(this.player, event.getPacket());
        for (final JavaPacketListener listener : PacketEvents.getApi().getJavaListeners()) {
            listener.packetSending(mcplEvent);
            event.setPacket(mcplEvent.getPacket());
        }

        event.setCancelled(mcplEvent.isCancelled());

        if (!event.isCancelled()) {
            listeners.forEach(l -> l.packetSending(event));
        }

        mcplEvent.getPostTasks().forEach(Runnable::run);
        mcplEvent.getPostTasks().clear();
    }

    @Override
    public void packetSent(Session session, Packet packet) {
        listeners.forEach(l -> l.packetSent(session, packet));
    }

    @Override
    public void connected(ConnectedEvent event) {
        listeners.forEach(l -> l.connected(event));
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        listeners.forEach(l -> l.disconnected(event));
    }

    @Override
    public void packetError(PacketErrorEvent event) {
        listeners.forEach(l -> l.packetError(event));
    }
}