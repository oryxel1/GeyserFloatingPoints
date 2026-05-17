package oxy.geyser.fp.session.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import oxy.geyser.fp.session.GeyserFPUser;

@RequiredArgsConstructor
public class ChunkCache {
    private final GeyserFPUser user;

    private final Long2ObjectMap<ChunkSection[]> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ClientboundLevelChunkWithLightPacket> chunkPackets = new Long2ObjectOpenHashMap<>();

    public void sendChunksWithOffset() {
        // This is a bad idea...
        for (Long2ObjectMap.Entry<ChunkSection[]> entry : this.chunks.long2ObjectEntrySet()) {
            final ClientboundLevelChunkWithLightPacket oldPacket = this.chunkPackets.get(entry.getLongKey());
            if (oldPacket == null) {
                continue;
            }

            ByteBuf oldByteBuf = Unpooled.wrappedBuffer(oldPacket.getChunkData());
            ByteBuf byteBuf = null;
            try {
                for (int sectionY = 0; sectionY < this.getChunkHeightY(); sectionY++) {
                    MinecraftTypes.readChunkSection(oldByteBuf, BlockRegistries.BLOCK_STATES.get().size(),
                            user.session().getRegistryCache().registry(JavaRegistries.BIOME).size());
                }

                byteBuf = ByteBufAllocator.DEFAULT.ioBuffer();

                ChunkSection[] sections = entry.getValue();
                for (ChunkSection section : sections) {
                    MinecraftTypes.writeChunkSection(byteBuf, section);
                }

                byteBuf.writeBytes(oldByteBuf);

                byte[] payload = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(payload);

                int x = (oldPacket.getX() << 4) - user.offset().getX();
                int z = (oldPacket.getZ() << 4) - user.offset().getZ();

                Registries.JAVA_PACKET_TRANSLATORS.translate(ClientboundLevelChunkWithLightPacket.class,
                        new ClientboundLevelChunkWithLightPacket(x >> 4, z >> 4, payload, oldPacket.getHeightMaps(), oldPacket.getBlockEntities(), oldPacket.getLightData()),
                        user.session(), true
                );
            } finally {
                if (byteBuf != null) {
                    byteBuf.release();
                }

                oldByteBuf.release();
            }
        }
    }

    public void addToCache(ClientboundLevelChunkWithLightPacket packet, ChunkSection[] chunks) {
        long chunkPosition = MathUtils.chunkPositionToLong(packet.getX(), packet.getZ());
        this.chunks.put(chunkPosition, chunks);
        this.chunkPackets.put(chunkPosition, packet);
    }

    private ChunkSection[] getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlockSections(ClientboundSectionBlocksUpdatePacket packet) {
        final ChunkSection[] column = this.getChunk(packet.getChunkX(), packet.getChunkZ());
        if (column == null) {
            return;
        }

        int y = packet.getChunkY() << 4;
        if (y < getMinY() || ((y - getMinY()) >> 4) > column.length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        ChunkSection palette = column[(y - getMinY()) >> 4];
        if (palette == null) {
            final JavaRegistry<Integer> registryKey = user.session().getRegistryCache().registry(JavaRegistries.BIOME);
            column[(y - getMinY()) >> 4] = palette = new ChunkSection(Block.JAVA_AIR_ID, BlockRegistries.BLOCK_STATES.get().size(), registryKey.values().getFirst(), registryKey.size());
        }

        for (BlockChangeEntry entry : packet.getEntries()) {
            palette.setBlock(entry.getPosition().getX() & 0xF,
                    entry.getPosition().getY() & 0xF, entry.getPosition().getZ() & 0xF, entry.getBlock());
        }
    }

    public void updateBlock(final Vector3i vector3i, int block) {
        this.updateBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ(), block);
    }

    public void updateBlock(int x, int y, int z, int block) {
        final ChunkSection[] chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        if (y < getMinY() || ((y - getMinY()) >> 4) > chunk.length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        ChunkSection section = chunk[(y - getMinY()) >> 4];
        if (section == null) {
            if (block != Block.JAVA_AIR_ID) {
                final JavaRegistry<Integer> registryKey = user.session().getRegistryCache().registry(JavaRegistries.BIOME);

                // A previously empty chunk, which is no longer empty as a block has been added to it
                // Fixes the chunk assuming that all blocks is the `block` variable we are updating. /shrug
                section = new ChunkSection(Block.JAVA_AIR_ID, BlockRegistries.BLOCK_STATES.get().size(), registryKey.values().getFirst(), registryKey.size());
                chunk[(y - getMinY()) >> 4] = section;
            } else {
                // Nothing to update
                return;
            }
        }

        section.setBlock(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public void remove(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        this.chunks.remove(chunkPosition);
        this.chunkPackets.remove(chunkPosition);
    }

    public int getMinY() {
        return (user.session().getChunkCache().getChunkMinY() << 4);
    }

    public int getChunkHeightY() {
        return user.session().getChunkCache().getChunkHeightY();
    }
}
