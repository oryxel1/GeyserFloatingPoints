package oxy.geyser.fp.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundInitializeBorderPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.border.ClientboundSetBorderCenterPacket;
import oxy.geyser.fp.network.event.JavaPacketEvent;
import oxy.geyser.fp.network.listener.JavaPacketListener;
import oxy.geyser.fp.session.GeyserFPUser;
import oxy.geyser.fp.world.ChunkRemapper;
import oxy.geyser.fp.world.VerticalWindow;

import java.util.ArrayList;
import java.util.List;

public class WorldPacketsRewriter implements JavaPacketListener {
    @Override
    public void onReceived(GeyserFPUser user, JavaPacketEvent event) {
        if (event.getPacket() instanceof ClientboundRespawnPacket
                || event.getPacket() instanceof ClientboundLoginPacket) {
            user.restoreGeyserOriginalValues();
            user.resetWorldHeight();
        }

        if (event.getPacket() instanceof ClientboundSetBorderCenterPacket packet) {
            event.setPacket(new ClientboundSetBorderCenterPacket(packet.getNewCenterX() - user.offset().getX(), packet.getNewCenterZ() - user.offset().getZ()));
        }

        if (event.getPacket() instanceof ClientboundInitializeBorderPacket packet) {
            event.setPacket(new ClientboundInitializeBorderPacket(
                    packet.getNewCenterX() - user.offset().getX(), packet.getNewCenterZ() - user.offset().getZ(),
                    packet.getOldSize(),
                    packet.getNewSize(),
                    packet.getLerpTime(),
                    packet.getNewAbsoluteMaxSize(),
                    packet.getWarningBlocks(),
                    packet.getWarningTime()
            ));
        }

        if (event.getPacket() instanceof ClientboundBlockEntityDataPacket packet) {
            event.setPacket(new ClientboundBlockEntityDataPacket(packet.getPosition().sub(user.offset()), packet.getType(), packet.getNbt()));
        }

        if (event.getPacket() instanceof ClientboundBlockEventPacket packet) {
            event.setPacket(new ClientboundBlockEventPacket(packet.getPosition().sub(user.offset()), packet.getRawType(), packet.getRawValue(), packet.getType(), packet.getValue(), packet.getBlockId()));
        }

        if (event.getPacket() instanceof ClientboundSetChunkCacheCenterPacket packet) {
            int x = (packet.getChunkX() << 4) - user.offset().getX();
            int z = (packet.getChunkZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundSetChunkCacheCenterPacket(x >> 4, z >> 4));
        }

        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket packet) {
            user.chunkCache().remove(packet.getX(), packet.getZ());

            int x = (packet.getX() << 4) - user.offset().getX();
            int z = (packet.getZ() << 4) - user.offset().getZ();
            event.setPacket(new ClientboundForgetLevelChunkPacket(x >> 4, z >> 4));
        }

        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            user.storeOriginalWorldHeight(
                    user.chunkCache().getChunkHeightY(),
                    user.chunkCache().getMinY()
            );

            final int javaChunkSize = user.originalChunkHeightY();
            final int worldMinY = user.originalMinY();
            final ChunkSection[] palette = new ChunkSection[javaChunkSize];
            final ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            try {
                for (int sectionY = 0; sectionY < javaChunkSize; sectionY++) {
                    palette[sectionY] = MinecraftTypes.readChunkSection(in, BlockRegistries.BLOCK_STATES.get().size(), user.session().getRegistryCache().registry(JavaRegistries.BIOME).size());
                }
            } finally {
                in.release();
            }
            user.chunkCache().addToCache(packet, palette);

            int x = (packet.getX() << 4) - user.offset().getX();
            int z = (packet.getZ() << 4) - user.offset().getZ();

            byte[] chunkData = packet.getChunkData();
            BlockEntityInfo[] blockEntities = packet.getBlockEntities();

            if (javaChunkSize > VerticalWindow.BEDROCK_SECTION_COUNT || user.offset().getY() != 0) {
                user.saveGeyserOriginalValues(
                        user.session().getChunkCache().getChunkMinY() << 4,
                        user.session().getChunkCache().getChunkHeightY() << 4
                );

                user.session().getChunkCache().setMinY(VerticalWindow.BEDROCK_MIN_Y);
                user.session().getChunkCache().setHeightY(VerticalWindow.BEDROCK_HEIGHT);

                ByteBuf newBuf = Unpooled.buffer();
                try {
                    ChunkRemapper.writeVisibleSections(newBuf, palette, worldMinY, user.offset().getY(), user.session());
                    chunkData = new byte[newBuf.readableBytes()];
                    newBuf.readBytes(chunkData);
                } finally {
                    newBuf.release();
                }

                blockEntities = ChunkRemapper.remapBlockEntities(packet.getBlockEntities(), user.offset().getY());
            }

            event.setPacket(new ClientboundLevelChunkWithLightPacket(x >> 4, z >> 4, chunkData, packet.getHeightMaps(), blockEntities, packet.getLightData()));
        }

        if (event.getPacket() instanceof ClientboundBlockUpdatePacket packet) {
            user.chunkCache().updateBlock(packet.getEntry().getPosition(), packet.getEntry().getBlock());
            event.setPacket(new ClientboundBlockUpdatePacket(new BlockChangeEntry(packet.getEntry().getPosition().sub(user.offset()), packet.getEntry().getBlock())));
        }

        if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
            user.chunkCache().updateBlockSections(packet);

            final List<BlockChangeEntry> entries = new ArrayList<>();
            for (BlockChangeEntry entry : packet.getEntries()) {
                user.chunkCache().updateBlock(entry.getPosition(), entry.getBlock());
                entries.add(new BlockChangeEntry(entry.getPosition().sub(user.offset()), entry.getBlock()));
            }

            int x = (packet.getChunkX() << 4) - user.offset().getX();
            int z = (packet.getChunkZ() << 4) - user.offset().getZ();
            int newChunkY = packet.getChunkY() - (user.offset().getY() >> 4);

            if (newChunkY >= 0 && newChunkY < VerticalWindow.BEDROCK_SECTION_COUNT) {
                event.setPacket(new ClientboundSectionBlocksUpdatePacket(x >> 4, newChunkY, z >> 4, entries.toArray(new BlockChangeEntry[0])));
            } else {
                event.cancel();
            }
        }
    }
}
