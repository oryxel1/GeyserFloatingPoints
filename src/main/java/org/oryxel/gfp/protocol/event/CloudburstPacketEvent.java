package org.oryxel.gfp.protocol.event;

import lombok.Data;

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.oryxel.gfp.session.CachedSession;

import java.util.ArrayList;
import java.util.List;

@Data
public class CloudburstPacketEvent {
    private final CachedSession session;
    private final BedrockPacket packet;
    private boolean cancelled;

    private final List<Runnable> postTasks = new ArrayList<>();
}