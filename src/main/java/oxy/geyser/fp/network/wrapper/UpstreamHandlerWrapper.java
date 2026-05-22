package oxy.geyser.fp.network.wrapper;

import lombok.NonNull;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.session.UpstreamSession;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.GeyserUtil;

public class UpstreamHandlerWrapper extends UpstreamSession {
    private final GeyserFPUser user;
    private final UpstreamSession prevSession;

    public UpstreamHandlerWrapper(GeyserFPUser user, BedrockServerSession session, UpstreamSession prevSession) {
        super(session);
        this.user = user;
        this.prevSession = prevSession;
    }

    @Override
    public void disconnect(String reason) {
        prevSession.disconnect(reason);
    }

    @Override
    public void sendPacket(@NonNull BedrockPacket packet) {
        if (packet instanceof StartGamePacket) {
            GeyserUtil.wrapAroundSessionAdaptor(user);
        }

        user.onPacketSent(packet);
        prevSession.sendPacket(packet);
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        user.onPacketSent(packet);
        prevSession.sendPacketImmediately(packet);
    }

    @Override
    public int getProtocolVersion() {
        return prevSession.getProtocolVersion();
    }
}

