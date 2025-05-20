package org.oryxel.gfp;

import lombok.Getter;
import org.oryxel.gfp.protocol.PacketEvents;

@Getter
public class GeyserFloatingPoints {
    @Getter
    private final static GeyserFloatingPoints instance = new GeyserFloatingPoints();
    private GeyserFloatingPoints() {}

    public void init() {
    }

    public void terminate() {
        PacketEvents.getApi().terminate();
    }
}
