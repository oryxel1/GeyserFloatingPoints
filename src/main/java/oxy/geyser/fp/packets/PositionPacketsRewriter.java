package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.SetTitlePacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.cache.WorldBorder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import oxy.geyser.fp.GeyserFloatingPoints;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.util.MathUtil;

import java.lang.reflect.Field;

public class PositionPacketsRewriter implements JavaPacketListener {
    @Override
    public void onSend(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPosPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel();
                return;
            }

            Vector3d vector3d = Vector3d.from(packet.getX() + user.offset().getX(),
                    packet.getY() + user.offset().getY(),
                    packet.getZ() + user.offset().getZ());

            event.setPacket(new ServerboundMovePlayerPosPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    vector3d.getX(), vector3d.getY(), vector3d.getZ()
            ));
        }

        if (event.getPacket() instanceof ServerboundMovePlayerPosRotPacket packet) {
            if (checkForReOffset(user, packet.getX(), packet.getY(), packet.getZ())) {
                event.cancel();
                return;
            }

            Vector3d vector3d = Vector3d.from(packet.getX() + user.offset().getX(),
                    packet.getY() + user.offset().getY(),
                    packet.getZ() + user.offset().getZ());

            event.setPacket(new ServerboundMovePlayerPosRotPacket(
                    packet.isOnGround(), packet.isHorizontalCollision(),
                    vector3d.getX(),
                    vector3d.getY(),
                    vector3d.getZ(), packet.getYaw(), packet.getPitch()
            ));
        }

        if (event.getPacket() instanceof ServerboundMoveVehiclePacket packet) {
            if (checkForReOffset(user, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ())) {
                event.cancel();
                return;
            }

            event.setPacket(new ServerboundMoveVehiclePacket(packet.getPosition().add(user.offset().toDouble()), packet.getYRot(), packet.getXRot(), packet.isOnGround()));
        }
    }

    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        final SessionPlayerEntity entity = user.session().getPlayerEntity();

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            Vector3f currentPositon = entity.getPosition().add(user.offset().toFloat());

            double x = packet.getPosition().getX() + (packet.getRelatives().contains(PositionElement.X) ? currentPositon.getX() : 0);
            double z = packet.getPosition().getZ() + (packet.getRelatives().contains(PositionElement.Z) ? currentPositon.getZ() : 0);

            final int CAPPED_VALUE = GeyserFloatingPoints.config().maxPosition();
            if (Math.abs(x - user.offset().getX()) > CAPPED_VALUE || Math.abs(z - user.offset().getZ()) > CAPPED_VALUE) {
                user.offset(MathUtil.calculateOffset(Vector3d.from(x, 0, z)), false);
            }

            // I don't want to deal with relatives.
            packet.getRelatives().remove(PositionElement.X);
            packet.getRelatives().remove(PositionElement.Z);

            event.setPacket(new ClientboundPlayerPositionPacket(
                    packet.getId(),
                    x - user.offset().getX(), packet.getPosition().getY(), z - user.offset().getZ(),
                    packet.getDeltaMovement().getX(), packet.getDeltaMovement().getY(), packet.getDeltaMovement().getZ(),
                    packet.getYRot(), packet.getXRot(), packet.getRelatives().toArray(new PositionElement[0])
            ));
        }
    }

    private boolean checkForReOffset(GeyserFPUser user, double x, double y, double z) {
        Vector3i pos = Vector3i.from(x, y, z);
        if (pos.distance(user.prevPosition) > 0 && user.shouldShowPosition()) {
            SetTitlePacket titlePacket = new SetTitlePacket();
            titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
            titlePacket.setText("XYZ: " + pos.add(user.offset()));
            titlePacket.setXuid("");
            titlePacket.setPlatformOnlineId("");
            user.session().sendUpstreamPacket(titlePacket);
        }

        final int CAPPED_VALUE = GeyserFloatingPoints.config().maxPosition();

        if (Math.abs(x) < CAPPED_VALUE && Math.abs(z) < CAPPED_VALUE) {
            return false;
        }

        user.offset(MathUtil.calculateOffset(Vector3d.from(x + user.offset().getX(), 0, z + user.offset().getZ())), true);
        return true;
    }
}
