package oxy.geyser.fp.util;

import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.UpstreamSession;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.event.session.SessionListener;
import oxy.geyser.fp.network.wrapper.GeyserFPAdapterWrapper;
import oxy.geyser.fp.network.wrapper.UpstreamHandlerWrapper;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.wrappers.GeyserBootstrapWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GeyserUtil {
    public static boolean wrapAroundGeyserBoostrap() {
        try {
            final Field field = GeyserImpl.class.getDeclaredField("bootstrap");
            field.setAccessible(true);
            field.set(GeyserImpl.getInstance(), new GeyserBootstrapWrapper(GeyserImpl.getInstance().getBootstrap()));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public static void wrapAroundSessionAdaptor(final GeyserFPUser user) {
        try {
            final ClientSession session = user.session().getDownstream().getSession();

            List<SessionListener> adapters = new ArrayList<>(session.getListeners());
            session.getListeners().forEach(session::removeListener);
            session.addListener(new GeyserFPAdapterWrapper(user, adapters));
        } catch (Exception ignored) {
            user.session().disconnect("Failed to wrap around session adaptor.");
        }
    }

    public static void wrapAroundUpstreamHandler(final GeyserFPUser user) {
        try {
            final BedrockServerSession session = user.session().getUpstream().getSession();
            final Field upstream = GeyserSession.class.getDeclaredField("upstream");
            upstream.setAccessible(true);
            upstream.set(user.session(), new UpstreamHandlerWrapper(user, session, (UpstreamSession) upstream.get(user.session())));
        } catch (Exception ignored) {
            user.session().disconnect("Failed to wrap around upstream handler.");
        }
    }
}
