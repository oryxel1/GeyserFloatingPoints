package org.oryxel.gfp.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.session.GeyserSession;
import org.oryxel.gfp.GeyserFloatingPoints;
import org.oryxel.gfp.session.CachedSession;

public class GFPExtension implements Extension {
    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        new CachedSession((GeyserSession) event.connection());
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
    }

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        GeyserFloatingPoints.getInstance().init();
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        GeyserFloatingPoints.getInstance().terminate();
    }
}
