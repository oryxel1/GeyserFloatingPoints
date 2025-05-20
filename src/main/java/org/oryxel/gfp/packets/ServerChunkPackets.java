package org.oryxel.gfp.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ServerChunkPackets implements JavaPacketListener {
    @Override
    public void packetReceived(Session session, MCPLPacketEvent event) {
        final CachedSession cached = event.getPlayer();

        if (event.getPacket() instanceof ClientboundSetChunkCacheCenterPacket packet) {
            int chunkBlockX = packet.getChunkX() << 4;
            int chunkBlockZ = packet.getChunkZ() << 4;

            chunkBlockX -= cached.getOffset().getFloorX();
            chunkBlockZ -= cached.getOffset().getFloorZ();

            event.setPacket(new ClientboundSetChunkCacheCenterPacket(chunkBlockX >> 4, chunkBlockZ >> 4));
        }

        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket packet) {
            cached.getChunkCache().removeChunk(packet.getX(), packet.getZ());

            // Ok now offset the chunk
            int chunkBlockX = packet.getX() << 4;
            int chunkBlockZ = packet.getZ() << 4;

            chunkBlockX -= cached.getOffset().getFloorX();
            chunkBlockZ -= cached.getOffset().getFloorZ();

            event.setPacket(new ClientboundForgetLevelChunkPacket(chunkBlockX >> 4, chunkBlockZ >> 4));
        }

        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            // Cache the chunk first.
            final int chunkSize = cached.getChunkCache().getChunkHeightY();
            final ChunkSection[] palette = new ChunkSection[chunkSize];
            final ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < chunkSize; sectionY++) {
                palette[sectionY] = MinecraftTypes.readChunkSection(in);
            }
            cached.getChunkCache().addToCache(packet, palette);

            // Ok now offset the chunk
            int chunkBlockX = packet.getX() << 4;
            int chunkBlockZ = packet.getZ() << 4;

            chunkBlockX -= cached.getOffset().getFloorX();
            chunkBlockZ -= cached.getOffset().getFloorZ();

            event.setPacket(new ClientboundLevelChunkWithLightPacket(
                    chunkBlockX >> 4, chunkBlockZ >> 4, packet.getChunkData(),
                    packet.getHeightMaps(), packet.getBlockEntities(), packet.getLightData()
            ));
        }

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
            cached.getChunkCache().updateBlock(packet.getEntry().getPosition(), packet.getEntry().getBlock());
        }

        if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
            for (BlockChangeEntry entry : packet.getEntries()) {
                cached.getChunkCache().updateBlock(entry.getPosition(), entry.getBlock());
            }
        }

        if (event.getPacket() instanceof ClientboundRespawnPacket) {
            cached.getChunkCache().loadDimension();
        }
    }
}
