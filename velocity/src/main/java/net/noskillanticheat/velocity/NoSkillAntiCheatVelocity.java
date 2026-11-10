package net.noskillanticheat.velocity;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.github.retrooper.packetevents.velocity.factory.VelocityPacketEventsBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * NoSkillAntiCheat Velocity add-on. Install it on the proxy only; the backend servers run the normal
 * NoSkillAntiCheat plugin. It hides the proxy software: the F3 brand becomes the configured name
 * (default "vanilla", without " (Velocity)") and the software name disappears from the server list.
 */
@Plugin(id = "noskillanticheat", name = "NoSkillAntiCheat-Velocity", version = "1.0.0",
        description = "Hides the proxy software (brand and server-list version) for NoSkillAntiCheat",
        authors = {"NoSkillAntiCheat contributors"})
public final class NoSkillAntiCheatVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private volatile String brand = "vanilla";
    private volatile boolean enabled = true;
    private volatile boolean cleanServerList = true;

    @Inject
    public NoSkillAntiCheatVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        loadConfig();
        PluginContainer container = server.getPluginManager().fromInstance(this).orElseThrow();
        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, container, logger, dataDirectory));
        PacketEvents.getAPI().getSettings().checkForUpdates(false);
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(new BrandListener(() -> enabled ? brand : null));
        PacketEvents.getAPI().init();
        logger.info("Hiding the proxy software: brand '{}', server list cleaned: {}", brand, cleanServerList);
    }

    @Subscribe
    public void onPing(ProxyPingEvent event) {
        if (!enabled || !cleanServerList) return;
        ServerPing ping = event.getPing();
        ServerPing.Version version = ping.getVersion();
        if (version == null) return;
        String cleaned = ProxyBrand.cleanVersionName(version.getName());
        if (cleaned.equals(version.getName())) return;
        event.setPing(ping.asBuilder().version(new ServerPing.Version(version.getProtocol(), cleaned)).build());
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (PacketEvents.getAPI() != null) PacketEvents.getAPI().terminate();
    }

    private void loadConfig() {
        Path file = dataDirectory.resolve("config.properties");
        Properties properties = new Properties();
        try {
            Files.createDirectories(dataDirectory);
            if (!Files.exists(file)) {
                properties.setProperty("enabled", "true");
                properties.setProperty("brand", "vanilla");
                properties.setProperty("clean-server-list", "true");
                try (OutputStream out = Files.newOutputStream(file)) {
                    properties.store(out, "NoSkillAntiCheat-Velocity: brand = name shown in F3 instead of '<server> (Velocity)'");
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                properties.load(in);
            }
        } catch (IOException e) {
            logger.warn("Could not read {}, using defaults: {}", file, e.getMessage());
        }
        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
        brand = properties.getProperty("brand", "vanilla").trim();
        cleanServerList = Boolean.parseBoolean(properties.getProperty("clean-server-list", "true"));
    }
}
