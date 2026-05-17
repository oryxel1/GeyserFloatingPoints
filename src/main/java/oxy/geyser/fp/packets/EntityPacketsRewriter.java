package oxy.geyser.fp.packets;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;

import java.util.ArrayList;
import java.util.Optional;

public class EntityPacketsRewriter implements JavaPacketListener {
    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            event.setPacket(new ClientboundAddEntityPacket(
                    packet.getEntityId(), packet.getUuid(), packet.getType(), packet.getData(),
                    packet.getX() - user.offset().getX(),
                    packet.getY(),
                    packet.getZ() - user.offset().getZ(),
                    packet.getMovement(), packet.getYaw(), packet.getHeadYaw(), packet.getPitch()
            ));
        }

        if (event.getPacket() instanceof ClientboundSetEntityDataPacket packet) {
            ArrayList<EntityMetadata<?, ?>> list = new ArrayList<>();

            for (EntityMetadata<?, ?> meta : packet.getMetadata()) {
                if (meta.getId() == 14 && meta.getType() == MetadataTypes.OPTIONAL_BLOCK_POS) {
                    Object value = meta.getValue();

                    if (value instanceof Optional<?> opt && opt.isPresent()) {
                        Object inner = opt.get();

                        if (inner instanceof Vector3i pos) {
                            list.add(new ObjectEntityMetadata<>(
                                    meta.getId(),
                                    MetadataTypes.OPTIONAL_BLOCK_POS,
                                    Optional.of(pos.sub(user.offset()))
                            ));
                            continue;
                        }
                    }
                }

                list.add(meta);
            }

            event.setPacket(new ClientboundSetEntityDataPacket(packet.getEntityId(), list.toArray(EntityMetadata[]::new)));
        }

        if (event.getPacket() instanceof ClientboundEntityPositionSyncPacket packet) {
            event.setPacket(new ClientboundEntityPositionSyncPacket(
                    packet.getId(), packet.getPosition().sub(user.offset().toDouble()),
                    packet.getDeltaMovement(), packet.getYRot(), packet.getXRot(), packet.isOnGround()
            ));
        }

        if (event.getPacket() instanceof ClientboundMoveVehiclePacket packet) {
            event.setPacket(new ClientboundMoveVehiclePacket(packet.getPosition().sub(user.offset().toDouble()),
                    packet.getYRot(), packet.getXRot()));
        }
    }
}
