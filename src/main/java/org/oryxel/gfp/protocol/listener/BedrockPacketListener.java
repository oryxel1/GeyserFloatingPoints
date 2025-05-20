package org.oryxel.gfp.protocol.listener;

import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;

public interface BedrockPacketListener {
    default void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
    }

    default void onPacketReceived(final CloudburstPacketEvent event) {
    }
}