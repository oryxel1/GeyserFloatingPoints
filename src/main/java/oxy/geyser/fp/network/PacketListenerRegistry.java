package oxy.geyser.fp.network;

import oxy.geyser.fp.network.listener.JavaPacketListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PacketListenerRegistry {
    private static final PacketListenerRegistry instance = new PacketListenerRegistry();
    public static PacketListenerRegistry instance() {
        return instance;
    }

    private PacketListenerRegistry() {
    }

    private final List<JavaPacketListener> javaPacketListeners = new ArrayList<>();
    public List<JavaPacketListener> javaListeners() {
        return Collections.unmodifiableList(this.javaPacketListeners);
    }

    public void register(JavaPacketListener... listeners) {
        this.javaPacketListeners.addAll(List.of(listeners));
    }
}
