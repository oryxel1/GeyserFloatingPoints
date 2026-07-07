package oxy.geyser.fp.world;

import io.netty.buffer.ByteBuf;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;

import java.util.ArrayList;
import java.util.List;

public final class ChunkRemapper {
    private ChunkRemapper() {
    }

    public static void writeVisibleSections(ByteBuf byteBuf, ChunkSection[] sections, int worldMinY, int offsetY, GeyserSession session) {
        int sourceStartSection = VerticalWindow.sourceStartSection(worldMinY, offsetY);

        final int blockStateCount = BlockRegistries.BLOCK_STATES.get().size();
        final JavaRegistry<Integer> biomeRegistry = session.getRegistryCache().registry(JavaRegistries.BIOME);
        final int biomeId = biomeRegistry.values().getFirst();
        final int biomeCount = biomeRegistry.size();

        for (int i = 0; i < VerticalWindow.BEDROCK_SECTION_COUNT; i++) {
            int sourceIndex = sourceStartSection + i;
            ChunkSection sourceSection = sourceIndex >= 0 && sourceIndex < sections.length ? sections[sourceIndex] : null;
            if (sourceSection != null && !sourceSection.isBlockCountEmpty()) {
                MinecraftTypes.writeChunkSection(byteBuf, sourceSection);
            } else {
                MinecraftTypes.writeChunkSection(byteBuf, forcedNetworkAirSection(
                        sourceSection, blockStateCount, biomeId, biomeCount));
            }
        }
    }

    public static BlockEntityInfo[] remapBlockEntities(BlockEntityInfo[] blockEntities, int offsetY) {
        List<BlockEntityInfo> remapped = new ArrayList<>();
        for (BlockEntityInfo blockEntity : blockEntities) {
            int fakeY = blockEntity.getY() - offsetY;
            if (fakeY < VerticalWindow.BEDROCK_MIN_Y || fakeY >= VerticalWindow.BEDROCK_MAX_Y_EXCLUSIVE) {
                continue;
            }

            NbtMap nbt = blockEntity.getNbt();
            if (nbt != null && nbt.containsKey("y")) {
                nbt = nbt.toBuilder().putInt("y", fakeY).build();
            }

            remapped.add(new BlockEntityInfo(
                    blockEntity.getX(), fakeY, blockEntity.getZ(), blockEntity.getType(), nbt));
        }
        return remapped.toArray(new BlockEntityInfo[0]);
    }

    private static ChunkSection forcedNetworkAirSection(ChunkSection sourceSection, int blockStateCount, int biomeId, int biomeCount) {
        return new ChunkSection(
                1,
                0,
                DataPalette.createForBlockState(Block.JAVA_AIR_ID, blockStateCount),
                sourceSection != null ? new DataPalette(sourceSection.getBiomeData()) : DataPalette.createForBiome(biomeId, biomeCount));
    }
}