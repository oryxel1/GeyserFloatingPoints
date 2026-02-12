package org.oryxel.gfp.handler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.api.block.custom.CustomBlockState;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.cache.BlockBreakHandler;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.geyser.translator.item.CustomItemTranslator;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.oryxel.gfp.session.CachedSession;

import java.util.Objects;

public class GFPBlockBreakHandler extends BlockBreakHandler {
    private final CachedSession cachedSession;
    public GFPBlockBreakHandler(CachedSession cachedSession) {
        super(cachedSession.getSession());
        this.cachedSession = cachedSession;
    }

    @Override
    protected void handleStartBreak(@NonNull Vector3i position, @NonNull BlockState state, Direction blockFace, long tick) {
        Vector3i offsetPos = position;
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            offsetPos = position.add(cachedSession.getOffset());
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, offsetPos);
        }

        GeyserItemStack item = session.getPlayerInventory().getItemInHand();

        // Account for fire - the client likes to hit the block behind.
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(offsetPos, blockFace);
        Block possibleFireBlock = session.getGeyser().getWorldManager().blockAt(session, fireBlockPos).block();
        if (possibleFireBlock == Blocks.FIRE || possibleFireBlock == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, BlockUtils.getBlockPosition(position, blockFace).add(cachedSession.getOffset()), blockFace.mcpl(), this.session.getWorldCache().nextPredictionSequence());
            this.session.sendDownstreamGamePacket(startBreakingPacket);
        }

        // % block breaking progress in this tick
        float breakProgress = calculateBreakProgress(state, position, item);

        // insta-breaking should be treated differently; don't send STOP_BREAK for these
        if (session.isInstabuild() || breakProgress >= 1.0F) {
            // Avoid sending STOP_BREAK for instantly broken blocks
            destroyBlock(state, position, blockFace, true);
            this.lastMinedPosition = position;
        } else {
            // If the block is custom or the breaking item is custom, we must keep track of break time ourselves
            ItemMapping mapping = item.getMapping(session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(session, item.getAmount(), item.getAllComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(state.javaId());
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(position);
            this.serverSideBlockBreaking = BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(state.javaId()) || blockStateOverride != null ||
                    customItem != null || session.getItemMappings().getNonVanillaCustomItemIds().contains(item.getJavaId()) || (skull != null && skull.getBlockDefinition() != null);

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(position.toFloat());
            startBreak.setData((int) (65535 / BlockUtils.reciprocal(breakProgress)));
            session.sendUpstreamPacket(startBreak);

            BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);

            this.currentBlockFace = blockFace;
            this.currentBlockPos = position;
            this.currentBlockState = state;
            this.currentItemStack = item;
            // The Java client calls MultiPlayerGameMode#startDestroyBlock which would set this to zero,
            // but also #continueDestroyBlock in the same tick to advance the break progress.
            this.currentProgress = breakProgress;

            session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position.add(cachedSession.getOffset()),
                    blockFace.mcpl(), session.getWorldCache().nextPredictionSequence()));
        }
    }

    @Override
    protected boolean canBreak(Vector3i vector, BlockState state, PlayerActionType action) {
        vector = vector.add(cachedSession.getOffset());
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, vector);
        }

        if (!this.session.isHandsBusy() && this.session.getWorldBorder().isInsideBorderBoundaries()) {
            switch (this.session.getGameMode()) {
                case SPECTATOR, ADVENTURE:
                    return super.canBreak(vector, state, action);
                default:
                    Vector3f playerPosition = this.session.getPlayerEntity().getPosition();
                    playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - this.session.getEyeHeight());
                    playerPosition = playerPosition.add(this.cachedSession.getOffset().toFloat());
                    return BedrockInventoryTransactionTranslator.canInteractWithBlock(this.session, playerPosition, vector);
            }
        } else {
            return false;
        }
    }

    @Override
    protected void destroyBlock(BlockState state, Vector3i vector, Direction direction, boolean instamine) {
        Vector3i offset = vector.add(cachedSession.getOffset());
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, offset);
        }

        this.session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(instamine ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING, offset, direction.mcpl(), this.session.getWorldCache().nextPredictionSequence()));
        this.session.getWorldCache().markPositionInSequence(offset);
        if (this.canDestroyBlock(state)) {
            BlockUtils.spawnBlockBreakParticles(this.session, direction, vector, state);
            BlockUtils.sendBedrockBlockDestroy(this.session, vector.toFloat(), state.javaId());
        } else {
            BlockUtils.restoreCorrectBlock(this.session, vector, state);
        }

        this.clearCurrentVariables();
    }

    @Override
    protected void handleContinueDestroy(@NonNull Vector3i position, @NonNull BlockState state, @NonNull Direction blockFace, boolean bedrockDestroyed, boolean sendParticles, long tick) {
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, position.add(cachedSession.getOffset()));
        }

        if (currentBlockState != null && Objects.equals(position, currentBlockPos) && sameItemStack()) {
            this.currentBlockFace = blockFace;

            final float newProgress = calculateBreakProgress(state, position, session.getPlayerInventory().getItemInHand());
            this.currentProgress = this.currentProgress + newProgress;
            double totalBreakTime = BlockUtils.reciprocal(newProgress);

            if (sendParticles || (serverSideBlockBreaking && currentProgress % 4 == 0)) {
                BlockUtils.spawnBlockBreakParticles(session, blockFace, position, state);
            }

            // let's be a bit lenient here; the Vanilla server is as well
            if (mayBreak(currentProgress, bedrockDestroyed)) {
                destroyBlock(state, position, blockFace, false);
                if (!bedrockDestroyed) {
                    // Only store it if we need to ignore subsequent Bedrock block actions
                    this.lastMinedPosition = position;
                }
                return;
            } else if (bedrockDestroyed) {
                BlockUtils.restoreCorrectBlock(session, position, state);
            }

            // Update the break time in the event that player conditions changed (jumping, effects applied)
            LevelEventPacket updateBreak = new LevelEventPacket();
            updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            updateBreak.setPosition(position.toFloat());
            updateBreak.setData((int) (65535 / totalBreakTime));
            session.sendUpstreamPacket(updateBreak);
        } else {
            // Don't store last mined position; we don't want to ignore any actions now that we switched!
            this.lastMinedPosition = null;
            // We have switched - either between blocks, or are between the stack we're using to break the block
            if (currentBlockPos != null) {
                LevelEventPacket updateBreak = new LevelEventPacket();
                updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
                updateBreak.setPosition(position.toFloat());
                updateBreak.setData(0);
                session.sendUpstreamPacketImmediately(updateBreak);

                // Prevent ghost blocks when Bedrock thinks it destroyed a block and wants to "move on",
                // while it wasn't actually destroyed on our end.
                if (bedrockDestroyed && currentBlockState != null) {
                    BlockUtils.restoreCorrectBlock(session, currentBlockPos, currentBlockState);
                }

                handleAbortBreaking(currentBlockPos);
            }

            handleStartBreak(position, state, blockFace, tick);
        }
    }

    private void handleAbortBreaking(Vector3i position) {
        // Bedrock edition "confirms" it stopped breaking blocks by sending an abort packet
        // We don't forward those as a Java client wouldn't send those either
        if (currentBlockPos != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, currentBlockPos.add(cachedSession.getOffset()),
                    Direction.DOWN.mcpl(), 0);
            session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(session, position.toFloat());
        this.clearCurrentVariables();
    }

    private boolean sameItemStack() {
        if (currentItemStack == null) {
            return false;
        }
        GeyserItemStack stack = session.getPlayerInventory().getItemInHand();
        if (currentItemStack.isEmpty() && stack.isEmpty()) {
            return true;
        }
        if (currentItemStack.getJavaId() != stack.getJavaId()) {
            return false;
        }

        return Objects.equals(stack.getComponents(), currentItemStack.getComponents());
    }
}
