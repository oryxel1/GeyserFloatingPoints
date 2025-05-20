package org.oryxel.gfp.geyser.util;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.protocol.mitm.CloudburstReceiveListener;
import org.oryxel.gfp.protocol.mitm.CloudburstSendListener;
import org.oryxel.gfp.protocol.mitm.MCPLMiddleListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GeyserUtil {
    public static void hookIntoCloudburstMC(final CachedSession session) {
        final GeyserConnection connection = session.getSession();

        try {
            session.cloudburstDownstream = findCloudburstSession(connection);

            injectCloudburstUpstream(session);
            injectCloudburstDownstream(session);
        } catch (Exception ignored) {
            session.kick("Failed to hook into cloudburst session!");
        }
    }

    public static void hookIntoMCPL(final CachedSession player) {
        try {
            final ClientSession session = findClientSession(player.getSession());

            List<SessionListener> adapters = new ArrayList<>(session.getListeners());
            session.getListeners().forEach(session::removeListener);
            session.addListener(new MCPLMiddleListener(player, adapters));

            player.mcplSession = session;
        } catch (Exception ignored) {
            player.kick("Failed to hook into mcpl session!");
        }
    }

    private static void injectCloudburstDownstream(final CachedSession player) {
        final BedrockServerSession session = player.cloudburstDownstream;
        final BedrockPacketHandler handler = session.getPacketHandler();
        session.setPacketHandler(player.downstreamPacketHandler = new CloudburstReceiveListener(player, handler));
    }

    private static void injectCloudburstUpstream(final CachedSession player) throws Exception {
        final BedrockServerSession session = player.cloudburstDownstream;
        final Field upstream = GeyserSession.class.getDeclaredField("upstream");
        upstream.setAccessible(true);
        upstream.set(player.getSession(), player.cloudburstUpstream = new CloudburstSendListener(player, session));
    }

    private static ClientSession findClientSession(final GeyserConnection connection) throws Exception {
        final Field upstream = GeyserSession.class.getDeclaredField("downstream");
        upstream.setAccessible(true);
        final Object session = upstream.get(connection);
        final Field field = session.getClass().getDeclaredField("session");
        field.setAccessible(true);
        return (ClientSession) field.get(session);
    }

    private static BedrockServerSession findCloudburstSession(final GeyserConnection connection) throws Exception {
        final Field upstream = GeyserSession.class.getDeclaredField("upstream");
        upstream.setAccessible(true);
        final Object session = upstream.get(connection);
        final Field field = session.getClass().getDeclaredField("session");
        field.setAccessible(true);
        return (BedrockServerSession) field.get(session);
    }
}