package org.oryxel.gfp.protocol.mitm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.protocol.PacketEvents;
import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;

@RequiredArgsConstructor
@Getter
public final class CloudburstReceiveListener implements BedrockPacketHandler {
    private final CachedSession player;
    private final BedrockPacketHandler oldHandler;

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final BedrockPacketListener listener : PacketEvents.getApi().getBedrockListeners()) {
            listener.onPacketReceived(event);
        }

        if (event.isCancelled()) {
            return PacketSignal.HANDLED;
        }

        return this.oldHandler.handlePacket(event.getPacket());
    }
}