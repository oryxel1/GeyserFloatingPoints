package oxy.geyser.fp.network.listener;

import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.session.GeyserFPUser;

public interface JavaPacketListener {
    default void onReceived(GeyserFPUser user, JavaPacketEvent event) {
    }

    default void onSend(GeyserFPUser user, JavaPacketEvent event) {
    }
}
