package org.oryxel.gfp.protocol;

import lombok.Getter;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class PacketEvents {
    @Getter
    private static final PacketEvents api = new PacketEvents();
    private PacketEvents() {}

    private final List<BedrockPacketListener> bedrockListeners = new ArrayList<>();
    private final List<JavaPacketListener> javaListeners = new ArrayList<>();

    public void register(final BedrockPacketListener... listener) {
        this.bedrockListeners.addAll(List.of(listener));
    }

    public void register(final JavaPacketListener... listener) {
        this.javaListeners.addAll(List.of(listener));
    }

    public void terminate() {
        this.bedrockListeners.clear();
        this.javaListeners.clear();
    }
}
