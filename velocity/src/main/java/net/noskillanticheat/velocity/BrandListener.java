package net.noskillanticheat.velocity;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;

import java.util.function.Supplier;

/** Replaces the brand Velocity sends to clients ("&lt;backend&gt; (Velocity)") with the configured name. */
final class BrandListener extends PacketListenerAbstract {
    private final Supplier<String> brand;

    BrandListener(Supplier<String> brand) {
        super(PacketListenerPriority.MONITOR);
        this.brand = brand;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        String name = brand.get();
        if (name == null) return;
        if (event.getPacketType() == PacketType.Play.Server.PLUGIN_MESSAGE) {
            WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
            if (isBrand(packet.getChannelName())) {
                packet.setData(ProxyBrand.encodeBrand(name));
                event.markForReEncode(true);
            }
        } else if (event.getPacketType() == PacketType.Configuration.Server.PLUGIN_MESSAGE) {
            WrapperConfigServerPluginMessage packet = new WrapperConfigServerPluginMessage(event);
            if (isBrand(packet.getChannelName())) {
                packet.setData(ProxyBrand.encodeBrand(name));
                event.markForReEncode(true);
            }
        }
    }

    private static boolean isBrand(String channel) {
        return "minecraft:brand".equals(channel) || "MC|Brand".equals(channel);
    }
}
