package org.oryxel.gfp.cache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.oryxel.gfp.session.CachedSession;
import org.oryxel.gfp.util.DimensionUtil;

import java.util.Map;

// https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/session/cache/ChunkCache.java
@RequiredArgsConstructor
public class ChunkCache {
    @Getter
    private final CachedSession session;
    private final Long2ObjectMap<ChunkSection[]> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<ClientboundLevelChunkWithLightPacket> chunkPackets = new Long2ObjectOpenHashMap<>();
    @Getter
    private int minY;
    private int heightY;

    public void sendChunksWithOffset() {
        int oldDimension = session.getSession().getBedrockDimension().bedrockId();

        // Use dimension switch to quickly reset all previous chunks.
        // Also since we can't switch to same dimension, we send a fake one THEN the real one.
        DimensionUtil.switchDimension(session, DimensionUtils.getTemporaryDimension(oldDimension, oldDimension), false);
        DimensionUtil.switchDimension(session, oldDimension, true);

        session.getSession().setSpawned(false);
        session.silentDimensionSwitch = true;

        // This is a bad idea...
        for (Long2ObjectMap.Entry<ChunkSection[]> entry : this.chunks.long2ObjectEntrySet()) {
            final ClientboundLevelChunkWithLightPacket oldPacket = this.chunkPackets.get(entry.getLongKey());
            if (oldPacket == null) {
                // Odd
                continue;
            }

            ByteBuf oldByteBuf = Unpooled.wrappedBuffer(oldPacket.getChunkData());
            ByteBuf byteBuf = null;
            try {
                for (int sectionY = 0; sectionY < this.getChunkHeightY(); sectionY++) {
                    MinecraftTypes.readChunkSection(oldByteBuf);
                }

                byteBuf = ByteBufAllocator.DEFAULT.ioBuffer();

                ChunkSection[] sections = entry.getValue();
                for (ChunkSection section : sections) {
                    MinecraftTypes.writeChunkSection(byteBuf, section);
                }

                byteBuf.writeBytes(oldByteBuf);

                byte[] payload = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(payload);

                // Yep.
                this.session.mcplSession.callPacketReceived(
                        new ClientboundLevelChunkWithLightPacket(oldPacket.getX(),
                                oldPacket.getZ(), payload, oldPacket.getHeightMaps(), oldPacket.getBlockEntities(), oldPacket.getLightData())
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

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return this.getChunk(chunkX >> 4, chunkZ >> 4) != null;
    }

    private ChunkSection[] getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlock(final Vector3i vector3i, int block) {
        this.updateBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ(), block);
    }

    public void updateBlock(int x, int y, int z, int block) {
        final ChunkSection[] chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        if (y < minY || ((y - minY) >> 4) > chunk.length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        ChunkSection section = chunk[(y - minY) >> 4];
        if (section == null) {
            if (block != Block.JAVA_AIR_ID) {
                section = new ChunkSection();
                // A previously empty chunk, which is no longer empty as a block has been added to it
                // Fixes the chunk assuming that all blocks is the `block` variable we are updating. /shrug
                section.getChunkData().getPalette().stateToId(Block.JAVA_AIR_ID);
                chunk[(y - minY) >> 4] = section;
            } else {
                // Nothing to update
                return;
            }
        }

        section.setBlock(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        this.chunks.remove(chunkPosition);
        this.chunkPackets.remove(chunkPosition);
    }

    public void loadDimension() {
        final JavaDimension dimension = session.getSession().getDimensionType();
        this.minY = dimension.minY();
        this.heightY = dimension.height();
    }

    public int getChunkMinY() {
        return minY >> 4;
    }

    public int getChunkHeightY() {
        return heightY >> 4;
    }
}