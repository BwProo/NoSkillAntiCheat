package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (against BedAura): ticks from placing a bed to right-clicking a bed, which
 * blows it up in the Nether and the End. Bed PvP by hand needs a moment to aim at the bed again;
 * BedAura does it within a tick or two, every time. Same idea as AnchorTiming.
 */
@CheckData(name = "BedTiming", stableKey = "nsac.combat.bed_timing", description = "Placed and detonated beds too fast and too consistently", experimental = true, decay = 0.25)
public class BedTiming extends Check implements PacketReceiveListener {
    private static final int MAX_GAP_TICKS = 40;
    private int ticksSincePlace = Integer.MAX_VALUE;
    private int[] gaps;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public BedTiming(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(5, Math.min(100, config.getIntElse(getConfigName() + ".sample-size", 10)));
        if (gaps == null || gaps.length != size) {
            gaps = new int[size];
            count = 0;
        }
        maxMeanTicks = config.getDoubleElse(getConfigName() + ".max-mean-ticks", 2.0);
        maxStdDevTicks = config.getDoubleElse(getConfigName() + ".max-std-dev", 1.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            String held = player.inventory.getItemInHand(packet.getHand()).getType().getName().getKey();
            boolean clickedBed = BlockTags.BEDS.contains(player.compensatedWorld.getBlock(packet.getBlockPosition()).getType());
            if (clickedBed && ticksSincePlace <= MAX_GAP_TICKS && !player.isSneaking) {
                add(ticksSincePlace);
                ticksSincePlace = Integer.MAX_VALUE;
            } else if (isBed(held)) {
                ticksSincePlace = 0;
            }
        } else if (isTickPacket(event.getPacketType()) && ticksSincePlace != Integer.MAX_VALUE) {
            ticksSincePlace++;
        }
    }

    private void add(int gap) {
        gaps[count++] = gap;
        if (count < gaps.length) return;
        ClickStats stats = ClickStats.of(gaps, count);
        count = 0;
        if (AnchorTiming.isBotLike(stats, maxMeanTicks, maxStdDevTicks)) {
            flag(String.format(Locale.ROOT, "meanGap=%.2f ticks std=%.2f n=%d", stats.meanTicks(), stats.stdDevTicks(), stats.samples()));
        } else {
            reward();
        }
    }

    public static boolean isBed(@NotNull String itemKey) {
        return itemKey.endsWith("_bed");
    }
}
