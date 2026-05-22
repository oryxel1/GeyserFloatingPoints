package oxy.geyser.fp.session;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.GameRulesChangedPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.EntityCache;
import org.geysermc.geyser.session.cache.TeleportCache;
import org.geysermc.geyser.session.cache.WorldBorder;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.session.cache.ChunkCache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

@RequiredArgsConstructor
public class GeyserFPUser {
    private boolean geyserShowCoordinates = true;
    private boolean currentlyHidingCoordinates = false;
    private static final String SHOW_COORDINATES_GAME_RULE = "showcoordinates";

    private final GeyserSession session;
    public GeyserSession session() {
        return session;
    }

    @Setter
    private boolean positionShown = GeyserFloatingPoints.config().showPositionByDefault();
    public boolean shouldShowPosition() {
        return this.positionShown;
    }

    private final ChunkCache chunkCache = new ChunkCache(this);
    public ChunkCache chunkCache() {
        return this.chunkCache;
    }

    private Vector3i offset = Vector3i.from(0, 0 ,0);
    public Vector3i offset() {
        return offset;
    }

    public Vector3i prevPosition = Vector3i.ZERO;

    public void offset(Vector3i offset, boolean teleport) {
        final SessionPlayerEntity entity = this.session.getPlayerEntity();

        // We have to set this first regardless of teleport so the chunks so up properly.
        entity.setPosition(entity.position().add(this.offset.toFloat()).sub(offset.toFloat()));

        if (teleport) {
            entity.moveAbsolute(
                    entity.position(),
                    entity.getYaw(), entity.getPitch(), entity.isOnGround(), true
            );

            session.setUnconfirmedTeleport(new TeleportCache(entity.position(), entity.getPitch(), entity.getYaw(), 0));
        }

        try {
            Field field = WorldBorder.class.getDeclaredField("center");
            field.setAccessible(true);
            final Vector2d center = (Vector2d) field.get(session.getWorldBorder());

            final WorldBorder border = session.getWorldBorder();
            border.setCenter(center.add(this.offset.getX(), this.offset.getZ()).sub(offset.getX(), offset.getZ()));

            border.update();
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        // This hacky mess is due to Geyser relocating stuff....
        try {
            final Field entitiesField = EntityCache.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
            final Object entities = entitiesField.get(session.getEntityCache());

            final Method valuesMethod = entities.getClass().getDeclaredMethod("values");
            valuesMethod.setAccessible(true);
            final Collection<Entity> values = (Collection<Entity>) valuesMethod.invoke(entities);

            for (Entity other : values) {
                if (other == entity) {
                    continue;
                }
                other.setPosition(other.position().add(this.offset.toFloat()).sub(offset.toFloat()));
                other.despawnEntity();
                other.spawnEntity();
            }
        } catch (Exception ignored) {
        }

        this.offset = offset;
        chunkCache.sendChunksWithOffset();
        updateCoordinatesGameRule();
    }

    public void onPacketSent(BedrockPacket packet) {
        if (packet instanceof GameRulesChangedPacket gameRulesPacket) {
            for (int i = 0; i < gameRulesPacket.getGameRules().size(); i++) {
                GameRuleData<?> rule = gameRulesPacket.getGameRules().get(i);
                if (!rule.getName().equals(SHOW_COORDINATES_GAME_RULE)) {
                    continue;
                }
                if (rule.getValue() instanceof Boolean bool && !currentlyHidingCoordinates) {
                    geyserShowCoordinates = bool;
                }
                if (currentlyHidingCoordinates) {
                    gameRulesPacket.getGameRules().set(i, new GameRuleData<>(SHOW_COORDINATES_GAME_RULE, false));
                }
                return;
            }
        }
    }

    private void updateCoordinatesGameRule() {
        boolean shouldHide = !this.offset.equals(Vector3i.ZERO);
        if (this.currentlyHidingCoordinates == shouldHide) {
            return;
        }
        this.currentlyHidingCoordinates = shouldHide;

        GameRulesChangedPacket packet = new GameRulesChangedPacket();
        packet.getGameRules().add(new GameRuleData<>(SHOW_COORDINATES_GAME_RULE, geyserShowCoordinates && !shouldHide));
        session.sendUpstreamPacket(packet);
    }
}

