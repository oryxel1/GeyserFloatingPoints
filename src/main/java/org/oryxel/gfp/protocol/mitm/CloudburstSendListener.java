package org.oryxel.gfp.protocol.mitm;

import lombok.NonNull;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.session.UpstreamSession;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.protocol.PacketEvents;
import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;
import org.oryxel.gfp.geyser.util.GeyserUtil;

public final class CloudburstSendListener extends UpstreamSession {
    private final CachedSession player;

    public CloudburstSendListener(CachedSession player, BedrockServerSession session) {
        super(session);
        this.player = player;
    }

    @Override
    public void sendPacket(@NonNull BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final BedrockPacketListener listener : PacketEvents.getApi().getBedrockListeners()) {
            listener.onPacketSend(event, false);
        }

        if (event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof StartGamePacket start) {
            GeyserUtil.hookIntoMCPL(this.player);

            // We need this to do rewind teleport.
            start.setAuthoritativeMovementMode(AuthoritativeMovementMode.SERVER_WITH_REWIND);
            start.setRewindHistorySize(20); // 20 ticks is enough.

            player.runtimeId = start.getRuntimeEntityId();
        }

        super.sendPacket(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        final CloudburstPacketEvent event = new CloudburstPacketEvent(this.player, packet);
        for (final BedrockPacketListener listener : PacketEvents.getApi().getBedrockListeners()) {
            listener.onPacketSend(event, true);
        }

        if (event.isCancelled()) {
            return;
        }

        super.sendPacketImmediately(event.getPacket());
        event.getPostTasks().forEach(Runnable::run);
        event.getPostTasks().clear();
    }
}