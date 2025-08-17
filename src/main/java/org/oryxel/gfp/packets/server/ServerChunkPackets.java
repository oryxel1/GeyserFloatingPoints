package org.oryxel.gfp.packets.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

import java.util.ArrayList;
import java.util.List;

public class ServerChunkPackets implements JavaPacketListener {
    @Override
    public void packetReceived(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket packet) {
            event.setPacket(new ClientboundBlockEntityDataPacket(packet.getPosition().sub(session.getOffset()), packet.getType(), packet.getNbt()));
        }

        if (event.getPacket() instanceof ClientboundBlockEventPacket packet) {
            event.setPacket(new ClientboundBlockEventPacket(packet.getPosition().sub(session.getOffset()), packet.getRawType(), packet.getRawValue(), packet.getType(), packet.getValue(), packet.getBlockId()));
        }

        if (event.getPacket() instanceof ClientboundSetChunkCacheCenterPacket packet) {
            int chunkBlockX = packet.getChunkX() << 4;
            int chunkBlockZ = packet.getChunkZ() << 4;

            chunkBlockX -= session.getOffset().getX();
            chunkBlockZ -= session.getOffset().getZ();

            event.setPacket(new ClientboundSetChunkCacheCenterPacket(chunkBlockX >> 4, chunkBlockZ >> 4));
        }

        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket packet) {
            session.getChunkCache().removeChunk(packet.getX(), packet.getZ());

            // Ok now offset the chunk
            int chunkBlockX = packet.getX() << 4;
            int chunkBlockZ = packet.getZ() << 4;

            chunkBlockX -= session.getOffset().getX();
            chunkBlockZ -= session.getOffset().getZ();

            event.setPacket(new ClientboundForgetLevelChunkPacket(chunkBlockX >> 4, chunkBlockZ >> 4));
        }

        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            // Cache the chunk first.
            final int chunkSize = session.getChunkCache().getChunkHeightY();
            final ChunkSection[] palette = new ChunkSection[chunkSize];
            final ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < chunkSize; sectionY++) {
                palette[sectionY] = MinecraftTypes.readChunkSection(in);
            }
            session.getChunkCache().addToCache(packet, palette);

            // Ok now offset the chunk
            int chunkBlockX = packet.getX() << 4;
            int chunkBlockZ = packet.getZ() << 4;

            chunkBlockX -= session.getOffset().getX();
            chunkBlockZ -= session.getOffset().getZ();

            event.setPacket(new ClientboundLevelChunkWithLightPacket(
                    chunkBlockX >> 4, chunkBlockZ >> 4, packet.getChunkData(),
                    packet.getHeightMaps(), packet.getBlockEntities(), packet.getLightData()
            ));
        }

        // System.out.println(event.getPacket());

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
            session.getChunkCache().updateBlock(packet.getEntry().getPosition(), packet.getEntry().getBlock());

            final BlockChangeEntry entry = packet.getEntry();
            event.setPacket(new ClientboundBlockUpdatePacket(new BlockChangeEntry(entry.getPosition().sub(session.getOffset()), entry.getBlock())));
        }

        if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
            final List<BlockChangeEntry> entries = new ArrayList<>();
            for (BlockChangeEntry entry : packet.getEntries()) {
                session.getChunkCache().updateBlock(entry.getPosition(), entry.getBlock());

                entries.add(new BlockChangeEntry(entry.getPosition().sub(session.getOffset()), entry.getBlock()));
            }

            event.setPacket(new ClientboundSectionBlocksUpdatePacket(packet.getChunkX(), packet.getChunkY(), packet.getChunkZ(), entries.toArray(new BlockChangeEntry[0])));
        }

        if (event.getPacket() instanceof ClientboundRespawnPacket) {
            session.getChunkCache().loadDimension();
        }
    }
}
