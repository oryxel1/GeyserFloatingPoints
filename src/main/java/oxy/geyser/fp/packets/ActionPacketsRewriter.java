package oxy.geyser.fp.packets;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemFromBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetJigsawBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetStructureBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundBlockEntityTagQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundJigsawGeneratePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;

public class ActionPacketsRewriter implements JavaPacketListener {
    @Override
    public void onSend(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundBlockEntityTagQueryPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundJigsawGeneratePacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundPickItemFromBlockPacket packet) {
            event.setPacket(packet.withPos(packet.getPos().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundSetCommandBlockPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundSetJigsawBlockPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundSetStructureBlockPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }

        if (event.getPacket() instanceof ServerboundSignUpdatePacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().add(user.offset())));
        }
    }

    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
            event.setPacket(packet.withPosition(packet.getPosition().sub(user.offset())));
        }
    }
}
