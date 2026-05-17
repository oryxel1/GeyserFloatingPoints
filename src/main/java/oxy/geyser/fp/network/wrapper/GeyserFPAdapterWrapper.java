package oxy.geyser.fp.network.wrapper;

import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import oxy.geyser.fp.network.PacketListenerRegistry;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;

import java.util.List;

@RequiredArgsConstructor
public class GeyserFPAdapterWrapper extends SessionAdapter {
    private final GeyserFPUser user;
    private final List<SessionListener> listeners;

    @Override
    public void packetReceived(Session session, Packet packet) {
        final JavaPacketEvent event = new JavaPacketEvent(packet);
        for (final JavaPacketListener listener : PacketListenerRegistry.instance().javaListeners()) {
            listener.onReceived(user, event);
        }
        if (!event.isCancelled()) {
            listeners.forEach(l -> l.packetReceived(session, event.getPacket()));
        }
    }

    @Override
    public void packetSending(PacketSendingEvent sendingEvent) {
        final JavaPacketEvent event = new JavaPacketEvent(sendingEvent.getPacket());
        for (final JavaPacketListener listener : PacketListenerRegistry.instance().javaListeners()) {
            listener.onSend(user, event);
        }
        sendingEvent.setPacket(event.getPacket());

        if (!event.isCancelled()) {
            listeners.forEach(l -> l.packetSending(sendingEvent));
        }
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
