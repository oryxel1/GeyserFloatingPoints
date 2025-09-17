package org.oryxel.gfp.handler;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
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

        GeyserItemStack item = this.session.getPlayerInventory().getItemInHand();
        Vector3i fireBlockPos = BlockUtils.getBlockPosition(offsetPos, blockFace);
        Block possibleFireBlock = this.session.getGeyser().getWorldManager().blockAt(this.session, fireBlockPos).block();
        if (possibleFireBlock == Blocks.FIRE || possibleFireBlock == Blocks.SOUL_FIRE) {
            ServerboundPlayerActionPacket startBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, BlockUtils.getBlockPosition(position, blockFace).add(cachedSession.getOffset()), blockFace.mcpl(), this.session.getWorldCache().nextPredictionSequence());
            this.session.sendDownstreamGamePacket(startBreakingPacket);
        }

        float breakProgress = this.calculateBreakProgress(state, position, item);
        if (!this.session.isInstabuild() && !(breakProgress >= 1.0F)) {
            ItemMapping mapping = item.getMapping(this.session);
            ItemDefinition customItem = mapping.isTool() ? CustomItemTranslator.getCustomItem(item.getComponents(), mapping) : null;
            CustomBlockState blockStateOverride = BlockRegistries.CUSTOM_BLOCK_STATE_OVERRIDES.get(state.javaId());
            SkullCache.Skull skull = this.session.getSkullCache().getSkulls().get(position);
            this.blockStartBreakTime = 0L;
            if (BlockRegistries.NON_VANILLA_BLOCK_IDS.get().get(state.javaId()) || blockStateOverride != null || customItem != null || skull != null && skull.getBlockDefinition() != null) {
                this.blockStartBreakTime = tick;
                this.lastBlockBreakFace = blockFace;
            }

            LevelEventPacket startBreak = new LevelEventPacket();
            startBreak.setType(LevelEvent.BLOCK_START_BREAK);
            startBreak.setPosition(position.toFloat());
            startBreak.setData((int)((double)65535.0F / BlockUtils.reciprocal(breakProgress)));
            this.session.sendUpstreamPacket(startBreak);
            BlockUtils.spawnBlockBreakParticles(this.session, blockFace, position, state);
            this.currentBlockPos = position;
            this.currentBlockState = state;
            this.session.sendDownstreamGamePacket(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, position.add(cachedSession.getOffset()), blockFace.mcpl(), this.session.getWorldCache().nextPredictionSequence()));
        } else {
            this.lastInstaMinedPosition = position;
            this.destroyBlock(state, position, blockFace, true);
        }
    }

    @Override
    protected boolean canBreak(Vector3i vector, BlockState state) {
        vector = vector.add(cachedSession.getOffset());
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, vector);
        }

        if (!this.session.isHandsBusy() && this.session.getWorldBorder().isInsideBorderBoundaries()) {
            switch (this.session.getGameMode()) {
                case SPECTATOR, ADVENTURE:
                    return super.canBreak(vector, state);
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
    protected void handleAbortBreaking(Vector3i position) {
        if (this.currentBlockPos != null) {
            ServerboundPlayerActionPacket abortBreakingPacket = new ServerboundPlayerActionPacket(PlayerAction.CANCEL_DIGGING, this.currentBlockPos.add(cachedSession.getOffset()), Direction.DOWN.mcpl(), 0);
            this.session.sendDownstreamGamePacket(abortBreakingPacket);
        }

        BlockUtils.sendBedrockStopBlockBreak(this.session, position.toFloat());
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
    protected void handleContinueDestroy(Vector3i position, BlockState state, Direction blockFace, long tick) {
        if (session.getGeyser().getWorldManager().hasOwnChunkCache()) {
            state = this.session.getGeyser().getWorldManager().blockAt(this.session, position.add(cachedSession.getOffset()));
        }

        BlockUtils.spawnBlockBreakParticles(this.session, blockFace, position, state);

        // Position value isn't needed here.
        double totalBreakTime = BlockUtils.reciprocal(this.calculateBreakProgress(state, Vector3i.ZERO, this.session.getPlayerInventory().getItemInHand()));
        if (this.blockStartBreakTime != 0L) {
            long ticksSinceStart = tick - this.blockStartBreakTime;
            if ((double)ticksSinceStart >= (totalBreakTime += 2.0F)) {
                this.destroyBlock(state, position, blockFace, false);
                return;
            }

            this.lastBlockBreakFace = blockFace;
        }

        LevelEventPacket updateBreak = new LevelEventPacket();
        updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        updateBreak.setPosition(position.toFloat());
        updateBreak.setData((int)(65535.0F / totalBreakTime));
        this.session.sendUpstreamPacket(updateBreak);
    }
}
