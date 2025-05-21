package org.oryxel.gfp.packets.client;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.mcprotocollib.network.Session;
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
import org.oryxel.gfp.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

public class ClientPositionPacket implements BedrockPacketListener, JavaPacketListener {
    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final CachedSession session = event.getPlayer();

        // We need to handle this before Geyser handle it.
        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            session.rotation = packet.getRotation();

            if (!session.getSession().isSpawned() || session.getSession().getUnconfirmedTeleport() != null) {
                // Not yet.
                return;
            }

            if (session.unconfirmedTeleport != null) {
                // Cough cough, well 10 blocks since unconfirmedTeleport will only be non-null if the teleport distance > 2000
                // so idc, also we check for this since the packet we send won't affect velocity lol.
                if (session.unconfirmedTeleport.distance(packet.getPosition()) <= 10) {
                    session.unconfirmedTeleport = null;
                }

                event.setCancelled(true);
                return;
            }

            if (Vector3f.from(packet.getPosition().getX(), 0, packet.getPosition().getZ()).length() < 2000) {
                return;
            }

            double oldPosX = Double.parseDouble(Float.toString(packet.getPosition().getX()));
            double oldPosZ = Double.parseDouble(Float.toString(packet.getPosition().getZ()));
            double newPosX = Math.sqrt(Math.abs(oldPosX)) * Math.signum(oldPosX);
            double newPosZ = Math.sqrt(Math.abs(oldPosZ)) * Math.signum(oldPosZ);

            event.setCancelled(true);

            Vector3i offset = MathUtil.makeOffsetChunkSafe(Vector3d.from(oldPosX - newPosX, 0, oldPosZ - newPosZ));

            newPosX = oldPosX - offset.getX();
            newPosZ = oldPosZ - offset.getZ();

            session.reOffsetPlayer(newPosX, newPosZ, offset);
        }
    }

    @Override
    public void packetSending(MCPLPacketEvent event) {
        final CachedSession cached = event.getPlayer();

        if (event.getPacket() instanceof ServerboundMovePlayerPosPacket packet) {
            event.setPacket(new ServerboundMovePlayerPosPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    packet.getX() + cached.getOffset().getX(),
                    packet.getY() + cached.getOffset().getY(),
                    packet.getZ() + cached.getOffset().getZ()
            ));
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPosRotPacket packet) {
            event.setPacket(new ServerboundMovePlayerPosRotPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    packet.getX() + cached.getOffset().getX(),
                    packet.getY() + cached.getOffset().getY(),
                    packet.getZ() + cached.getOffset().getZ(), packet.getYaw(), packet.getPitch()
            ));
        }

        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet) {
            event.setPacket(new ServerboundMoveVehiclePacket(packet.getPosition().add(cached.getOffset().toDouble()), packet.getYRot(), packet.getXRot(), packet.isOnGround()));
        }
    }

    @Override
    public void packetReceived(Session session, MCPLPacketEvent event) {
        final CachedSession cached = event.getPlayer();
        final SessionPlayerEntity entity = cached.getSession().getPlayerEntity();

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            Vector3d pos = packet.getPosition();

            cached.unconfirmedTeleport = null; // No need for this anymore.

            double realX = pos.getX() + (packet.getRelatives().contains(PositionElement.X) ? entity.getPosition().getX() : 0),
                    realZ = pos.getZ() + (packet.getRelatives().contains(PositionElement.Z) ? entity.getPosition().getZ() : 0);
            double newX = realX, newZ = realZ;

            // Too close, ignore.
            if (entity.getPosition().distance(realX, entity.getPosition().getY(), realZ) < 0.1) {
                return;
            }

            if (Math.abs(newX) > 2000) {
                newX = Math.sqrt(Math.abs(newX)) * Math.signum(realX);
            }

            if (Math.abs(newZ) > 2000) {
                newZ = Math.sqrt(Math.abs(newZ)) * Math.signum(realZ);
            }

            final Vector3i oldOffset = cached.getOffset();
            cached.setOffset(MathUtil.makeOffsetChunkSafe(Vector3d.from(realX - newX, 0, realZ - newZ)));
            if (cached.getOffset().lengthSquared() > 0) { // Always priority 0 0 0 offset.
                double oldOffsetX = realX - oldOffset.getX();
                double oldOffsetZ = realZ - oldOffset.getZ();

                // Old offset is still within range so there is no need to update it.
                if (Math.abs(oldOffsetX) <= 2000 && Math.abs(oldOffsetZ) <= 2000) {
                    cached.setOffset(oldOffset);
                }
            }

            newX = realX - cached.getOffset().getX();
            newZ = realZ - cached.getOffset().getZ();

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
            if (oldOffset.getX() == cached.getOffset().getX() && oldOffset.getY() == cached.getOffset().getY() &&
                    oldOffset.getZ() == cached.getOffset().getZ()) {
                return;
            }

            event.getPostTasks().add(() -> {
                // Offset changed and the server won't do it for us... Manually update chunk, and entity position.
                cached.getChunkCache().sendChunksWithOffset(oldOffset);
                cached.getEntityCache().resendWithOffset();
            });
        }
    }
}
