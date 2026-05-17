package oxy.geyser.fp.wrappers;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.configuration.GeyserConfig;
import org.geysermc.geyser.dump.BootstrapDumpInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.geysermc.geyser.util.metrics.MetricsPlatform;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.file.Path;

public record GeyserBootstrapWrapper(GeyserBootstrap bootstrap) implements GeyserBootstrap {
    @Override
    public void onGeyserInitialize() {
        bootstrap.onGeyserInitialize();
    }

    @Override
    public void onGeyserEnable() {
        bootstrap.onGeyserEnable();
    }

    @Override
    public void onGeyserDisable() {
        bootstrap.onGeyserDisable();
    }

    @Override
    public void onGeyserShutdown() {
        bootstrap.onGeyserShutdown();
    }

    @Override
    public @NonNull PlatformType platformType() {
        return bootstrap.platformType();
    }

    @Override
    public GeyserConfig config() {
        return bootstrap.config();
    }

    @Override
    public GeyserLogger getGeyserLogger() {
        return bootstrap.getGeyserLogger();
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return bootstrap.getCommandRegistry();
    }

    @Override
    public @Nullable IGeyserPingPassthrough getGeyserPingPassthrough() {
        return bootstrap.getGeyserPingPassthrough();
    }

    @Override
    public Path getConfigFolder() {
        return bootstrap.getConfigFolder();
    }

    @Override
    public BootstrapDumpInfo getDumpInfo() {
        return bootstrap.getDumpInfo();
    }

    @Override
    public @NonNull String getServerPlatform() {
        return bootstrap.getServerPlatform();
    }

    @Override
    public @NonNull String getServerBindAddress() {
        return bootstrap.getServerBindAddress();
    }

    @Override
    public int getServerPort() {
        return bootstrap.getServerPort();
    }

    @Override
    public boolean testFloodgatePluginPresent() {
        return bootstrap.testFloodgatePluginPresent();
    }

    @Override
    public Path getFloodgateKeyPath() {
        return bootstrap.getFloodgateKeyPath();
    }

    @Override
    public @Nullable String getMinecraftServerVersion() {
        return bootstrap.getMinecraftServerVersion();
    }

    @Override
    public @Nullable SocketAddress getSocketAddress() {
        return bootstrap.getSocketAddress();
    }

    @Override
    public Path getLogsPath() {
        return bootstrap.getLogsPath();
    }

    @Override
    public @Nullable InputStream getResourceOrNull(String resource) {
        return bootstrap.getResourceOrNull(resource);
    }

    @Override
    public @NonNull InputStream getResourceOrThrow(@NonNull String resource) {
        return bootstrap.getResourceOrThrow(resource);
    }

    @Override
    public <T extends GeyserConfig> T loadConfig(Class<T> configClass) {
        return bootstrap.loadConfig(configClass);
    }

    @Override
    public @Nullable MetricsPlatform createMetricsPlatform() {
        return bootstrap.createMetricsPlatform();
    }
}
