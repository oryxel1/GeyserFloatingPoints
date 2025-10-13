package org.oryxel.gfp.packets.server;

import org.cloudburstmc.protocol.bedrock.packet.ChangeDimensionPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetSpawnPositionPacket;
import org.geysermc.geyser.util.DimensionUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerLookAtPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;
import org.oryxel.gfp.protocol.event.CloudburstPacketEvent;
import org.oryxel.gfp.protocol.event.MCPLPacketEvent;
import org.oryxel.gfp.protocol.listener.BedrockPacketListener;
import org.oryxel.gfp.protocol.listener.JavaPacketListener;
import org.oryxel.gfp.session.CachedSession;

public class ServerWorldPackets implements JavaPacketListener, BedrockPacketListener {
    @Override
    public void packetReceived(MCPLPacketEvent event) {
        final CachedSession session = event.getSession();

        if (event.getPacket() instanceof ClientboundSoundPacket packet) {
            event.setPacket(new ClientboundSoundPacket(
                    packet.getSound(), packet.getCategory(),
                    packet.getX() - session.getOffset().getX(),
                    packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getVolume(), packet.getPitch(), packet.getSeed()
            ));
        }

        if (event.getPacket() instanceof ClientboundLevelEventPacket packet) {
            event.setPacket(new ClientboundLevelEventPacket(packet.getEvent(), packet.getPosition().sub(session.getOffset()), packet.getData(), packet.isBroadcast()));
        }

        if (event.getPacket() instanceof ClientboundLevelParticlesPacket packet) {
            event.setPacket(new ClientboundLevelParticlesPacket(packet.getParticle(), packet.isLongDistance(),
                    packet.isAlwaysShow(), packet.getX() - session.getOffset().getX(), packet.getY(), packet.getZ() - session.getOffset().getZ(),
                    packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ(), packet.getVelocityOffset(), packet.getAmount()));
        }

        if (event.getPacket() instanceof ClientboundExplodePacket packet) {
            event.setPacket(new ClientboundExplodePacket(packet.getCenter().sub(session.getOffset().toDouble()),
                    packet.getRadius(), packet.getBlockCount(),
                    packet.getPlayerKnockback(), packet.getExplosionParticle(), packet.getExplosionSound(), packet.getBlockParticles()));
        }

        if (event.getPacket() instanceof ClientboundOpenSignEditorPacket packet) {
            event.setPacket(new ClientboundOpenSignEditorPacket(packet.getPosition().sub(session.getOffset()), packet.isFrontText()));
        }

        if (event.getPacket() instanceof ClientboundPlayerLookAtPacket packet) {
            event.setPacket(new ClientboundPlayerLookAtPacket(packet.getOrigin(), packet.getX() - session.getOffset().getX(), packet.getY(), packet.getZ() - session.getOffset().getZ()));
        }

        if (event.getPacket() instanceof ClientboundSetDefaultSpawnPositionPacket packet) {
            session.worldSpawn = packet.getGlobalPos().getPosition();

            SetSpawnPositionPacket spawnPositionPacket = new SetSpawnPositionPacket();
            spawnPositionPacket.setBlockPosition(packet.getGlobalPos().getPosition().sub(session.getOffset()));
            // uhm...
            spawnPositionPacket.setDimensionId(session.getSession().getBedrockDimension().bedrockId());
            spawnPositionPacket.setSpawnType(SetSpawnPositionPacket.Type.WORLD_SPAWN);

            session.getSession().sendUpstreamPacket(spawnPositionPacket);
            event.setCancelled(true);
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        if (event.getPacket() instanceof ChangeDimensionPacket) {
            event.getSession().silentDimensionSwitch = false;
        }
    }
}
