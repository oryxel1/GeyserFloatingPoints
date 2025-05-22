package org.oryxel.gfp.packets.client;

import org.cloudburstmc.protocol.bedrock.data.*;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundPickItemFromBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetStructureBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundBlockEntityTagQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundJigsawGeneratePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundSignUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ClientPlayerAction implements JavaPacketListener, BedrockPacketListener {
    @Override
    public void packetSending(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet) {
            PlayerAction exemptedAction = session.getSession().getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;

            // Geyser is using their own chunk cache, which should mean the position match our offset position therefore, we only have to modify post translation
            if (session.getSession().getGeyser().getWorldManager().hasOwnChunkCache() && packet.getAction() != exemptedAction && packet.getAction() != PlayerAction.CANCEL_DIGGING) {
                return;
            }

            if (packet.getAction() != PlayerAction.START_DIGGING && packet.getAction() != PlayerAction.FINISH_DIGGING && packet.getAction() != PlayerAction.CANCEL_DIGGING) {
                return;
            }

            event.setPacket(new ServerboundPlayerActionPacket(packet.getAction(), packet.getPosition().add(session.getOffset()), packet.getFace(), packet.getSequence()));
        }

        if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            event.setPacket(new ServerboundUseItemOnPacket(
                    packet.getPosition().add(session.getOffset()), packet.getFace(), packet.getHand(), packet.getCursorX(),
                    packet.getCursorY(), packet.getCursorZ(), packet.isInsideBlock(), packet.isHitWorldBorder(), packet.getSequence()
            ));
        }

        if (event.getPacket() instanceof ServerboundBlockEntityTagQueryPacket packet) {
            event.setPacket(new ServerboundBlockEntityTagQueryPacket(packet.getTransactionId(), packet.getPosition().sub(session.getOffset())));
        }

        if (event.getPacket() instanceof ServerboundJigsawGeneratePacket packet) {
            event.setPacket(new ServerboundJigsawGeneratePacket(packet.getPosition().sub(session.getOffset()), packet.getLevels(), packet.isKeepJigsaws()));
        }

        if (event.getPacket() instanceof ServerboundPickItemFromBlockPacket packet) {
            event.setPacket(new ServerboundPickItemFromBlockPacket(packet.getPos().sub(session.getOffset()), packet.isIncludeData()));
        }

        if (event.getPacket() instanceof ServerboundSetCommandBlockPacket packet) {
            event.setPacket(new ServerboundSetCommandBlockPacket(packet.getPosition().sub(session.getOffset()), packet.getCommand(),
                    packet.getMode(), packet.isDoesTrackOutput(), packet.isConditional(), packet.isAutomatic()));
        }

        // A bit tricky so this is a TODO for now since net.kyori.adventure.key.Key is relocated.
//        if (event.getPacket() instanceof ServerboundSetJigsawBlockPacket packet) {
//            event.setPacket(new ServerboundSetJigsawBlockPacket(packet.getPosition().sub(session.getOffset()),
//                    packet.getName(), ));
//        }

        if (event.getPacket() instanceof ServerboundSetStructureBlockPacket packet) {
            event.setPacket(new ServerboundSetStructureBlockPacket(
                    packet.getPosition().sub(session.getOffset()), packet.getAction(), packet.getMode(),
                    packet.getName(), packet.getOffset(), packet.getSize(),  packet.getMirror(),
                    packet.getRotation(), packet.getMetadata(), packet.getIntegrity(), packet.getSeed(),
                    packet.isIgnoreEntities(), packet.isShowAir(), packet.isShowBoundingBox(), packet.isStrict()
            ));
        }

        if (event.getPacket() instanceof ServerboundSignUpdatePacket packet) {
            event.setPacket(new ServerboundSignUpdatePacket(packet.getPosition().sub(session.getOffset()), packet.getLines(), packet.isFrontText()));
        }
    }

    @Override
    public void packetReceived(MCPLPacketEvent event) {
        if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
            event.setPacket(new ClientboundBlockDestructionPacket(packet.getBreakerEntityId(),
                    packet.getPosition().sub(event.getSession().getOffset()), packet.getStage()));
        }
    }

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        if (event.getPacket() instanceof PlayerAuthInputPacket packet) {
            final CachedSession session = event.getSession();

            if (!session.getSession().getGeyser().getWorldManager().hasOwnChunkCache()) {
                // Handled in packetSending.
                return;
            }

            if (!packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS)) {
                return;
            }

            // Look at BedrockBlockActions line 90, Geyser is getting blockId using the server world manager.
            // which means the position ain't offset, while the one player sending Geyser is offset.
            // Therefore, we have to handle this first since if blockId is air (since the position we send Geyser is invalid)
            // else Geyser will never translate it to ServerboundPlayerActionPacket!
            for (PlayerBlockActionData blockActionData : packet.getPlayerActions()) {
                // Only needed to do this for START_BREAK, for CONTINUE_BREAK for example we need to use the offset position
                // so Geyser send back the correct level event.
                if (blockActionData.getBlockPosition() != null && blockActionData.getAction() == PlayerActionType.START_BREAK) {
                    blockActionData.setBlockPosition(blockActionData.getBlockPosition().add(session.getOffset()));
                }
            }
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof LevelEventPacket packet) {
            if (!session.getSession().getGeyser().getWorldManager().hasOwnChunkCache()) {
                // Handled in packetSending.
                return;
            }

            // Since we un-offset the START_BREAK (look at this class line 117) we need to "re-offset" it.
            if (packet.getType() == LevelEvent.BLOCK_START_BREAK) {
                packet.setPosition(packet.getPosition().sub(session.getOffset().toFloat()));
            }
        }
    }
}
