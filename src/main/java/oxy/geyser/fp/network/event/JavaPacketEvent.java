package oxy.geyser.fp.network.event;

import lombok.Data;
import org.geysermc.mcprotocollib.network.packet.Packet;

@Data
public class JavaPacketEvent {
    private Packet packet;
    private boolean cancelled;

    public JavaPacketEvent(Packet packet) {
        this.packet = packet;
    }

    public void cancel() {
        this.cancelled = true;
    }
}
