package org.oryxel.gfp.geyser;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.session.GeyserSession;
import org.oryxel.gfp.GeyserFloatingPoints;
import org.oryxel.gfp.config.Config;
import org.oryxel.gfp.config.ConfigLoader;
import org.oryxel.gfp.session.CachedSession;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GFPExtension implements Extension {
    public static Config config;
    public static final List<GeyserSession> showPositions = new CopyOnWriteArrayList<>();

    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        new CachedSession((GeyserSession) event.connection());
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
        showPositions.remove((GeyserSession) event.connection());
    }

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        config = ConfigLoader.load(this, GFPExtension.class, Config.class);
        GeyserFloatingPoints.getInstance().init();
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        GeyserFloatingPoints.getInstance().terminate();
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this).source(CommandSource.class)
                .name("position")
                .playerOnly(true).bedrockOnly(true)
                .description("Toggle off/on title to show your real position, won't show anything if your current position is in fact real.")
                .executor((source, cmd, args) -> {
                    if (source.connection() instanceof GeyserSession session) {
                        if (showPositions.contains(session)) {
                            showPositions.remove(session);
                            source.sendMessage("Stop showing your position!");
                        } else {
                            showPositions.add(session);
                            source.sendMessage("Now showing your position!");
                        }
                    }
                })
                .build());
    }
}
