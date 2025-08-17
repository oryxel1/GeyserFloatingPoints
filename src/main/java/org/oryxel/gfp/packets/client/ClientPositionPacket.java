package org.oryxel.gfp.packets.client;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.ServerboundLoadingScreenPacketType;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerboundLoadingScreenPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.util.DimensionUtil;
import org.oryxel.gfp.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientPositionPacket implements BedrockPacketListener, JavaPacketListener {
    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ServerboundLoadingScreenPacket packet && packet.getType() == ServerboundLoadingScreenPacketType.END_LOADING_SCREEN) {
            if (Objects.equals(DimensionUtil.LOADING_SCREEN_ID, packet.getLoadingScreenId()) && session.silentDimensionSwitch) {
                session.getSession().setSpawned(true);
                session.silentDimensionSwitch = false;

                // Hacky fix so that player keep the old motion, would be better if you use rewind correction but too lazy to implement that
                SetEntityMotionPacket motion = new SetEntityMotionPacket();
                motion.setMotion(session.cachedVelocity);
                motion.setRuntimeEntityId(session.runtimeId);

                session.cloudburstDownstream.sendPacketImmediately(motion);
            }
        }

        // We need to handle this before Geyser handle it.
        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            session.rotation = packet.getRotation();

            if (!session.getSession().isSpawned() || session.getSession().getUnconfirmedTeleport() != null) {
                // Not yet.
                return;
            }

            if (session.silentDimensionSwitch) {
                event.setCancelled(true);
                return;
            }

            // This move ain't valid for re offset!
            if (!isValidMove(session.getSession(), session.getSession().getPlayerEntity().getPosition(), packet.getPosition())) {
                return;
            }

            session.cachedVelocity = packet.getDelta();
            // Since Geyser no longer knows about our real position, we do the border check ourselves instead.
            if (session.getOffset().lengthSquared() > 0) {
                if (session.getSession().getWorldBorder().isPassingIntoBorderBoundaries(packet.getPosition().add(session.getOffset().toFloat()), true)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (Vector3f.from(packet.getPosition().getX(), 0, packet.getPosition().getZ()).length() < 2000) {
                return;
            }

            double oldPosX = Double.parseDouble(Float.toString(packet.getPosition().getX())) + session.getOffset().getX();
            double oldPosZ = Double.parseDouble(Float.toString(packet.getPosition().getZ())) + session.getOffset().getZ();
            double newPosX = MathUtil.findNewPosition(oldPosX);
            double newPosZ = MathUtil.findNewPosition(oldPosZ);

            event.setCancelled(true);

            Vector3i offset = MathUtil.makeOffsetChunkSafe(Vector3d.from(oldPosX - newPosX, 0, oldPosZ - newPosZ));

            newPosX = oldPosX - offset.getX();
            newPosZ = oldPosZ - offset.getZ();

            session.reOffsetPlayer(newPosX, newPosZ, offset);
        }
    }

    @Override
    public void packetSending(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ServerboundMovePlayerPosPacket packet) {
            event.setPacket(new ServerboundMovePlayerPosPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    packet.getX() + session.getOffset().getX(),
                    packet.getY() + session.getOffset().getY(),
                    packet.getZ() + session.getOffset().getZ()
            ));
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPosRotPacket packet) {
            event.setPacket(new ServerboundMovePlayerPosRotPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    packet.getX() + session.getOffset().getX(),
                    packet.getY() + session.getOffset().getY(),
                    packet.getZ() + session.getOffset().getZ(), packet.getYaw(), packet.getPitch()
            ));
        }

        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet) {
            event.setPacket(new ServerboundMoveVehiclePacket(packet.getPosition().add(session.getOffset().toDouble()), packet.getYRot(), packet.getXRot(), packet.isOnGround()));
        }
    }

    @Override
    public void packetReceived(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();
        final SessionPlayerEntity entity = session.getSession().getPlayerEntity();

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            Vector3d pos = packet.getPosition();
            Vector3f realPlayerPosition = entity.getPosition().add(session.getOffset().toFloat());

            double realX = pos.getX() + (packet.getRelatives().contains(PositionElement.X) ? realPlayerPosition.getX() : 0),
                    realZ = pos.getZ() + (packet.getRelatives().contains(PositionElement.Z) ? realPlayerPosition.getZ() : 0);
            double newX = realX, newZ = realZ;

            if (Math.abs(newX) > 2000) {
                newX = MathUtil.findNewPosition(newX);
            }

            if (Math.abs(newZ) > 2000) {
                newZ = MathUtil.findNewPosition(newZ);
            }

            final Vector3i oldOffset = session.getOffset();
            final Vector3i newOffset = MathUtil.makeOffsetChunkSafe(Vector3d.from(realX - newX, 0, realZ - newZ));

            session.setOffset(newOffset);
            if (newOffset.getX() != 0 || newOffset.getY() != 0 || newOffset.getZ() != 0) { // Always priority 0 0 0 offset.
                double oldOffsetX = realX - oldOffset.getX();
                double oldOffsetZ = realZ - oldOffset.getZ();

                // Old offset is still within range so there is no need to update it.
                if (Math.abs(oldOffsetX) <= 2000 && Math.abs(oldOffsetZ) <= 2000) {
                    session.setOffset(oldOffset);
                }
            }

            newX = realX - session.getOffset().getX();
            newZ = realZ - session.getOffset().getZ();

            // I don't want to deal with relatives, too lazy brah.
            List<PositionElement> relatives = new ArrayList<>();
            packet.getRelatives().forEach(r -> {
                if (r != PositionElement.X && r != PositionElement.Z) {
                    relatives.add(r);
                }
            });

            event.setPacket(new ClientboundPlayerPositionPacket(
                    packet.getId(),
                    newX, packet.getPosition().getY(), newZ,
                    packet.getDeltaMovement().getX(), packet.getDeltaMovement().getY(), packet.getDeltaMovement().getZ(),
                    packet.getYRot(), packet.getXRot(), relatives.toArray(new PositionElement[0])
            ));

            // Match 1 : 1, no need to update chunks.
            if (oldOffset.getX() == session.getOffset().getX() && oldOffset.getZ() == session.getOffset().getZ()) {
                return;
            }

            // Player haven't spawned yet, no need for manual update.
            if (!session.getSession().isSpawned()) {
                return;
            }

            event.getPostTasks().add(() -> {
                // Offset changed and the server won't do it for us... Manually update chunk, and entity position.
                session.getChunkCache().sendChunksWithOffset(oldOffset);
                session.getEntityCache().resendWithOffset();
                session.sendWorldSpawn();
            });
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final CachedSession session = event.getSession();
        if (event.getPacket() instanceof SetEntityMotionPacket packet && packet.getRuntimeEntityId() == session.runtimeId) {
            session.cachedVelocity = packet.getMotion();
        }
    }

    private static boolean isValidMove(GeyserSession session, Vector3f currentPosition, Vector3f newPosition) {
        if (isInvalidNumber(newPosition.getX()) || isInvalidNumber(newPosition.getY()) || isInvalidNumber(newPosition.getZ())) {
            return false;
        }
        if (currentPosition.distanceSquared(newPosition) > 15) {
            session.getGeyser().getLogger().debug(ChatColor.RED + session.bedrockUsername() + " moved too quickly." +
                    " current position: " + currentPosition + ", new position: " + newPosition);

            return false;
        }

        return true;
    }

    private static boolean isInvalidNumber(float val) {
        return Float.isNaN(val) || Float.isInfinite(val);
    }
}
