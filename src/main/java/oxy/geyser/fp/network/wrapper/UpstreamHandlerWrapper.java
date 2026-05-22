package oxy.geyser.fp.network.wrapper;

import lombok.NonNull;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.session.UpstreamSession;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.GeyserUtil;

import static oxy.geyser.fp.session.GeyserFPUser.SHOW_COORDINATES_GAME_RULE;

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

        hideCoordinatesIfOffset(packet);
        prevSession.sendPacket(packet);
    }

    @Override
    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        prevSession.sendPacketImmediately(packet);
    }

    @Override
    public int getProtocolVersion() {
        return prevSession.getProtocolVersion();
    }

    private void hideCoordinatesIfOffset(BedrockPacket packet) {
        if (packet instanceof GameRulesChangedPacket gameRulesPacket) {
            if (user.offset().equals(Vector3i.ZERO)) {
                return;
            }
            for (int i = 0; i < gameRulesPacket.getGameRules().size(); i++) {
                GameRuleData<?> rule = gameRulesPacket.getGameRules().get(i);
                if (!SHOW_COORDINATES_GAME_RULE.equals(rule.getName())) {
                    continue;
                }
                gameRulesPacket.getGameRules().set(i, new GameRuleData<>(SHOW_COORDINATES_GAME_RULE, false));
                return;
            }
        }
    }
}
