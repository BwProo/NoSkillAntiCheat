package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * NoSkillAntiCheat: flags clients that identify themselves as a known hacked client, through
 * their brand or the plugin channels their mods register. The punishment (by default a ban) is
 * set in punishments.yml under "BlacklistedClient".
 *
 * <p>Limits: a client can hide or fake both values, so this only catches clients that reveal
 * themselves. Patterns are configurable under {@code BlacklistedClient.patterns}; with
 * {@code BlacklistedClient.log-client-info: true} every join logs brand and channels, so you can
 * see what a client really sends before adding a pattern.
 */
@CheckData(name = "BlacklistedClient", stableKey = "nsac.client.blacklisted", description = "Joined with a blacklisted client or mod")
public class BlacklistedClient extends Check implements PacketReceiveListener {
    private ClientSignatures signatures;
    private boolean logClientInfo;
    private boolean flagged;
    // Brand and channels arrive in the configuration phase, before the player is in the world.
    // The flag (and with it the ban command) waits for the first play-phase packet so the
    // player is online and "ban <name>" can resolve them.
    private String pendingReason;

    public BlacklistedClient(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        signatures = new ClientSignatures(config.getStringListElse(getConfigName() + ".patterns", ClientSignatures.DEFAULT_PATTERNS));
        if (!signatures.invalidPatterns().isEmpty()) {
            LogUtil.warn("Ignoring invalid BlacklistedClient patterns: " + signatures.invalidPatterns());
        }
        logClientInfo = config.getBooleanElse(getConfigName() + ".log-client-info", true);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (pendingReason != null && event.getPacketType() instanceof PacketType.Play.Client) {
            String reason = pendingReason;
            pendingReason = null;
            flag(reason);
        }
        String channel;
        byte[] data;
        if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            channel = packet.getChannelName();
            data = packet.getData();
        } else if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            channel = packet.getChannelName();
            data = packet.getData();
        } else {
            return;
        }
        if (signatures == null || data == null) return;

        if (channel.equals("minecraft:brand") || channel.equals("MC|Brand")) {
            String brand = ClientSignatures.decodeBrand(data);
            if (logClientInfo) LogUtil.info("[ClientInfo] " + player.getName() + " brand=" + brand);
            check("brand", brand);
        } else if (channel.equals("minecraft:register") || channel.equals("REGISTER")) {
            List<String> channels = ClientSignatures.decodeRegister(data);
            if (logClientInfo) LogUtil.info("[ClientInfo] " + player.getName() + " channels=" + channels);
            for (String registered : channels) {
                if (check("channel", registered)) break;
            }
        } else {
            // Mods can also talk on their own channels without registering them first
            check("channel", channel);
        }
    }

    /** Flags a client detected by another probe (e.g. the sign translation probe), once. */
    public void flagDetected(@NotNull String reason) {
        if (flagged) return;
        flagged = true;
        LogUtil.info("[ClientInfo] " + player.getName() + " detected: " + reason);
        flag(reason);
    }

    private boolean check(String kind, String value) {
        if (flagged) return true;
        String pattern = signatures.match(value);
        if (pattern == null) return false;
        flagged = true;
        pendingReason = kind + "=" + value + " (pattern " + pattern + ")";
        LogUtil.info("[ClientInfo] " + player.getName() + " matched blacklisted client: " + pendingReason);
        return true;
    }
}
