package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.AttackPackets;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * NoSkillAntiCheat (CrystalAura / AutoCrystal): reaction time from "the client sees the end crystal"
 * to "the client hits it", in client ticks. The start is the client's answer to a transaction sent
 * right after the spawn packet, so ping does not count. Auras break a crystal in the tick they see
 * it, every time; even fast crystal PvP by hand needs a reaction and varies.
 */
@CheckData(name = "CrystalReaction", stableKey = "nsac.crystal.reaction", description = "Broke end crystals the moment they appeared, every time", experimental = true, decay = 0.25)
public class CrystalReaction extends Check implements PacketReceiveListener, PacketSendListener {
    private static final int MAX_DELAY_TICKS = 20;
    private final Map<Integer, Integer> seenAtTick = new HashMap<>();
    private int tick;
    private int[] delays;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public CrystalReaction(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(5, Math.min(100, config.getIntElse(getConfigName() + ".sample-size", 10)));
        if (delays == null || delays.length != size) {
            delays = new int[size];
            count = 0;
        }
        maxMeanTicks = config.getDoubleElse(getConfigName() + ".max-mean-ticks", 1.0);
        maxStdDevTicks = config.getDoubleElse(getConfigName() + ".max-std-dev", 0.6);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SPAWN_ENTITY) return;
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
        if (spawn.getEntityType() != EntityTypes.END_CRYSTAL) return;
        int id = spawn.getEntityId();
        player.addRealTimeTaskNext(() -> {
            if (seenAtTick.size() > 256) seenAtTick.clear();
            seenAtTick.put(id, tick);
        });
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTickPacket(event.getPacketType())) {
            tick++;
            return;
        }
        int attacked = AttackPackets.attackedEntity(event);
        if (attacked == AttackPackets.NONE) return;
        Integer seen = seenAtTick.remove(attacked);
        if (seen == null) return;
        int delay = tick - seen;
        if (delay < 0 || delay > MAX_DELAY_TICKS) return; // not a fresh crystal
        delays[count++] = delay;
        if (count < delays.length) return;
        ClickStats stats = ClickStats.of(delays, count);
        count = 0;
        if (isInstant(stats, maxMeanTicks, maxStdDevTicks)) {
            flag(String.format(Locale.ROOT, "meanReaction=%.2f ticks std=%.2f n=%d", stats.meanTicks(), stats.stdDevTicks(), stats.samples()));
        } else {
            reward();
        }
    }

    /** Pure rule, unit-tested. */
    public static boolean isInstant(@NotNull ClickStats stats, double maxMeanTicks, double maxStdDevTicks) {
        return stats.samples() > 0 && stats.meanTicks() <= maxMeanTicks && stats.stdDevTicks() <= maxStdDevTicks;
    }
}
