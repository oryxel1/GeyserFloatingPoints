package org.oryxel.gfp.packets.client;

import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
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
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ClientPlayerAction implements JavaPacketListener {
    @Override
    public void packetSending(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            PlayerAction action = packet.getAction();

            if (action == PlayerAction.START_DIGGING || action == PlayerAction.FINISH_DIGGING || action == PlayerAction.CANCEL_DIGGING) {
                return;
            }

            event.setPacket(new ServerboundPlayerActionPacket(packet.getAction(),
                    packet.getPosition().add(session.getOffset()), packet.getFace(), packet.getSequence()));
        }

        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            event.setPacket(new ServerboundUseItemOnPacket(
                    packet.getPosition().add(session.getOffset()), packet.getFace(), packet.getHand(), packet.getCursorX(),
                    packet.getCursorY(), packet.getCursorZ(), packet.isInsideBlock(), packet.isHitWorldBorder(), packet.getSequence()
            ));
        }

        if (event.getPacket() instanceof ServerboundBlockEntityTagQueryPacket packet) {
            event.setPacket(new ServerboundBlockEntityTagQueryPacket(packet.getTransactionId(), packet.getPosition().add(session.getOffset())));
        }

        if (event.getPacket() instanceof ServerboundJigsawGeneratePacket packet) {
            event.setPacket(new ServerboundJigsawGeneratePacket(packet.getPosition().add(session.getOffset()), packet.getLevels(), packet.isKeepJigsaws()));
        }

        if (event.getPacket() instanceof ServerboundPickItemFromBlockPacket packet) {
            event.setPacket(new ServerboundPickItemFromBlockPacket(packet.getPos().add(session.getOffset()), packet.isIncludeData()));
        }

        if (event.getPacket() instanceof ServerboundSetCommandBlockPacket packet) {
            event.setPacket(new ServerboundSetCommandBlockPacket(packet.getPosition().add(session.getOffset()), packet.getCommand(),
                    packet.getMode(), packet.isDoesTrackOutput(), packet.isConditional(), packet.isAutomatic()));
        }

        if (event.getPacket() instanceof ServerboundSetJigsawBlockPacket packet) {
            event.setPacket(new ServerboundSetJigsawBlockPacket(
                    packet.getPosition().add(session.getOffset()), packet.getName(), packet.getTarget(), packet.getPool(),
                    packet.getFinalState(), packet.getJointType(), packet.getSelectionPriority(), packet.getPlacementPriority()));
        }

        if (event.getPacket() instanceof ServerboundSetStructureBlockPacket packet) {
            event.setPacket(new ServerboundSetStructureBlockPacket(
                    packet.getPosition().add(session.getOffset()), packet.getAction(), packet.getMode(),
                    packet.getName(), packet.getOffset(), packet.getSize(), packet.getMirror(),
                    packet.getRotation(), packet.getMetadata(), packet.getIntegrity(), packet.getSeed(),
                    packet.isIgnoreEntities(), packet.isShowAir(), packet.isShowBoundingBox(), packet.isStrict()
            ));
        }

        if (event.getPacket() instanceof ServerboundSignUpdatePacket packet) {
            event.setPacket(new ServerboundSignUpdatePacket(packet.getPosition().add(session.getOffset()), packet.getLines(), packet.isFrontText()));
        }
    }

    @Override
    public void packetReceived(MCPLPacketEvent event) {
        if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
            event.setPacket(new ClientboundBlockDestructionPacket(packet.getBreakerEntityId(),
                    packet.getPosition().sub(event.getSession().getOffset()), packet.getStage()));
        }
    }
}
