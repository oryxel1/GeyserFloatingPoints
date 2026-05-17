package oxy.geyser.fp;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;
import oxy.geyser.fp.network.PacketListenerRegistry;
import oxy.geyser.fp.packets.ActionPacketsRewriter;
import oxy.geyser.fp.packets.EntityPacketsRewriter;
import oxy.geyser.fp.packets.PositionPacketsRewriter;
import oxy.geyser.fp.packets.WorldPacketsRewriter;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.GeyserUtil;
import oxy.geyser.fp.util.config.Config;
import oxy.geyser.fp.util.config.ConfigLoader;

import java.util.HashMap;
import java.util.Map;

public class GeyserFloatingPoints implements Extension {
    private static Config CONFIG;
    public static Config config() {
        return CONFIG;
    }

    private static final Map<GeyserSession, GeyserFPUser> SESSION_TO_GFP = new HashMap<>();

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        CONFIG = ConfigLoader.load(this, GeyserFloatingPoints.class, Config.class);

        PacketListenerRegistry.instance().register(new WorldPacketsRewriter());
        PacketListenerRegistry.instance().register(new PositionPacketsRewriter());
        PacketListenerRegistry.instance().register(new EntityPacketsRewriter());
        PacketListenerRegistry.instance().register(new ActionPacketsRewriter());

        // This will force Geyser into using the chunk cache for platform like Spigot so the world manager pick up the correct block.
        GeyserUtil.wrapAroundGeyserBoostrap();
    }

    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        final GeyserSession session = (GeyserSession) event.connection();
        final GeyserFPUser user = new GeyserFPUser(session);
        GeyserUtil.wrapAroundUpstreamHandler(user);

        SESSION_TO_GFP.put(session, user);
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
        SESSION_TO_GFP.remove((GeyserSession) event.connection());
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this).source(CommandSource.class)
                .name("position")
                .playerOnly(true).bedrockOnly(true).permission("geyserfloatingpoints.position", TriState.TRUE)
                .description("Toggle off/on title to show your real position, won't show anything if your current position is in fact real.")
                .executor((source, cmd, args) -> {
                    if (source.connection() instanceof GeyserSession session) {
                        final GeyserFPUser user = SESSION_TO_GFP.get(session);
                        if (user == null) {
                            return;
                        }
                        user.setPositionShown(!user.shouldShowPosition());
                        if (!user.shouldShowPosition()) {
                            source.sendMessage("Stop showing your current position.");
                        } else {
                            source.sendMessage("You should now be able to see your current position.");
                        }
                    }
                })
                .build());
    }
}
