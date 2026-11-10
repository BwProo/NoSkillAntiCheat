package ac.grim.grimac.manager.protection;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonObject;


/**
 * NoSkillAntiCheat: hides the server software. Rewrites the brand the client shows in F3
 * ("Purpur", "Paper" ...) and strips the software name from the version text in the server list.
 * Config: server-brand.enabled, server-brand.name (default "vanilla").
 */
public final class BrandSpoofer extends PacketListenerAbstract {
    public BrandSpoofer() {
        super(PacketListenerPriority.MONITOR);
    }

    private static ConfigManager config() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        ConfigManager config = config();
        if (!config.getBooleanElse("server-brand.enabled", true)) return;
        String brand = config.getStringElse("server-brand.name", "vanilla");
        if (event.getPacketType() == PacketType.Play.Server.PLUGIN_MESSAGE) {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
            if (isBrand(packet.getChannelName())) {
                packet.setData(ServerBrand.encodeBrand(brand));
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Configuration.Server.PLUGIN_MESSAGE) {
            WrapperConfigServerPluginMessage packet = new WrapperConfigServerPluginMessage(event);
            if (isBrand(packet.getChannelName())) {
                packet.setData(ServerBrand.encodeBrand(brand));
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Status.Server.RESPONSE) {
            WrapperStatusServerResponse packet = new WrapperStatusServerResponse(event);
            JsonObject json = packet.getComponent();
            if (json != null && json.has("version") && json.get("version").isJsonObject()) {
                JsonObject version = json.getAsJsonObject("version");
                if (version.has("name")) {
                    version.addProperty("name", ServerBrand.cleanVersionName(version.get("name").getAsString()));
                    packet.setComponent(json);
                    event.markForReEncode(true);
                }
            }
        }
    }

    private static boolean isBrand(String channel) {
        return "minecraft:brand".equals(channel) || "MC|Brand".equals(channel);
    }
}
