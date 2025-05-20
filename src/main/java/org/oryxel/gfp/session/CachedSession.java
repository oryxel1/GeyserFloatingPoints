package org.oryxel.gfp.session;

import lombok.Getter;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.oryxel.gfp.geyser.util.GeyserUtil;
import org.oryxel.gfp.protocol.mitm.CloudburstReceiveListener;
import org.oryxel.gfp.protocol.mitm.CloudburstSendListener;

public class CachedSession {
    @Getter
    private final GeyserSession session;

    public  ClientSession mcplSession;
    public BedrockServerSession cloudburstDownstream;
    public CloudburstSendListener cloudburstUpstream;
    public CloudburstReceiveListener downstreamPacketHandler;

    public CachedSession(GeyserSession session) {
        this.session = session;

        GeyserUtil.hookIntoCloudburstMC(this);
    }

    public void kick(String reason) {
        this.session.disconnect(reason);
    }
}
