package org.oryxel.gfp;

import lombok.Getter;
import org.oryxel.gfp.packets.ClientPositionPacket;
import org.oryxel.gfp.packets.ServerChunkPackets;
import org.oryxel.gfp.packets.ServerEntityPackets;
import org.oryxel.gfp.protocol.PacketEvents;

@Getter
public class GeyserFloatingPoints {
    @Getter
    private final static GeyserFloatingPoints instance = new GeyserFloatingPoints();
    private GeyserFloatingPoints() {}

    public void init() {
        PacketEvents.getApi().registerBedrock(new ClientPositionPacket());
        PacketEvents.getApi().registerJava(new ClientPositionPacket());

        PacketEvents.getApi().registerJava(new ServerChunkPackets());
        PacketEvents.getApi().registerJava(new ServerEntityPackets());
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
    }
}
