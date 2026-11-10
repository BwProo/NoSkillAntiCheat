package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Set;

/**
 * NoSkillAntiCheat: FastUse / ExpThrower / AutoMend / AutoExp. Holding right click throws an
 * item every 4 ticks in vanilla; spam-clicking by hand is irregular and rarely faster than every
 * second tick. These modules remove the delay and throw every tick, very evenly. Measured over
 * windows of throws in client ticks, so lag and ping do not matter.
 */
@CheckData(name = "FastUse", stableKey = "nsac.misc.fast_use", description = "Threw items every tick, faster and more evenly than a human can click", experimental = true, decay = 0.25)
public class FastUse extends Check implements PacketReceiveListener {
    private static final Set<String> THROWABLES = Set.of("experience_bottle", "snowball", "egg", "blue_egg",
            "brown_egg", "splash_potion", "lingering_potion", "wind_charge");
    private static final int MAX_GAP_TICKS = 10;
    private int ticksSinceUse = Integer.MAX_VALUE;
    private int[] gaps;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public FastUse(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(5, Math.min(100, config.getIntElse(getConfigName() + ".sample-size", 15)));
        if (gaps == null || gaps.length != size) {
            gaps = new int[size];
            count = 0;
        }
        maxMeanTicks = config.getDoubleElse(getConfigName() + ".max-mean-ticks", 1.25);
        maxStdDevTicks = config.getDoubleElse(getConfigName() + ".max-std-dev", 0.45);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            String key = player.inventory.getItemInHand(packet.getHand()).getType().getName().getKey();
            if (!THROWABLES.contains(key)) return;
            if (ticksSinceUse <= MAX_GAP_TICKS) {
                add(ticksSinceUse);
            } else {
                count = 0; // a new burst
            }
            ticksSinceUse = 0;
        } else if (isTickPacket(event.getPacketType()) && ticksSinceUse != Integer.MAX_VALUE) {
            ticksSinceUse++;
        }
    }

    private void add(int gap) {
        gaps[count++] = gap;
        if (count < gaps.length) return;
        ClickStats stats = ClickStats.of(gaps, count);
        count = 0;
        if (isBotLike(stats, maxMeanTicks, maxStdDevTicks)) {
            flag(String.format(Locale.ROOT, "meanGap=%.2f ticks std=%.2f n=%d", stats.meanTicks(), stats.stdDevTicks(), stats.samples()));
        } else {
            reward();
        }
    }

    public static boolean isThrowable(@NotNull String itemKey) {
        return THROWABLES.contains(itemKey);
    }

    /** Pure rule, unit-tested. */
    public static boolean isBotLike(@NotNull ClickStats stats, double maxMeanTicks, double maxStdDevTicks) {
        return stats.samples() > 0 && stats.meanTicks() <= maxMeanTicks && stats.stdDevTicks() <= maxStdDevTicks;
    }
}
