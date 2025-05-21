package org.oryxel.gfp;

import lombok.Getter;
import org.oryxel.gfp.packets.client.ClientPlayerAction;
import org.oryxel.gfp.packets.client.ClientPositionPacket;
import org.oryxel.gfp.packets.server.ServerChunkPackets;
import org.oryxel.gfp.packets.server.ServerEntityPackets;
import org.oryxel.gfp.protocol.PacketEvents;

@Getter
public class GeyserFloatingPoints {
    @Getter
    private final static GeyserFloatingPoints instance = new GeyserFloatingPoints();
    private GeyserFloatingPoints() {}

    public void init() {
        PacketEvents.getApi().registerBedrock(new ClientPositionPacket());
        PacketEvents.getApi().registerJava(new ClientPositionPacket());

        PacketEvents.getApi().registerJava(new ClientPlayerAction());
        PacketEvents.getApi().registerBedrock(new ClientPlayerAction());

        PacketEvents.getApi().registerJava(new ServerChunkPackets());
        PacketEvents.getApi().registerJava(new ServerEntityPackets());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
    }
}
