package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (against AnchorAura-style cheats): measures the ticks from placing a respawn
 * anchor to detonating a respawn anchor (right-clicking it with something other than glowstone).
 * By hand this needs placing, switching to glowstone, charging, switching away and clicking again;
 * automated anchor PvP does the whole sequence in a couple of ticks, every time.
 */
@CheckData(name = "AnchorTiming", stableKey = "nsac.anchor.timing", description = "Placed, charged and detonated respawn anchors too fast and too consistently", experimental = true, decay = 0.25)
public class AnchorTiming extends Check implements PacketReceiveListener {
    private static final int MAX_GAP_TICKS = 40;
    private int ticksSincePlace = Integer.MAX_VALUE;
    private int[] gaps;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public AnchorTiming(GrimPlayer player) {
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
            ItemStack held = player.inventory.getItemInHand(packet.getHand());
            if (held.is(ItemTypes.RESPAWN_ANCHOR)) {
                ticksSincePlace = 0;
                return;
            }
            if (ticksSincePlace > MAX_GAP_TICKS) return;
            if (player.compensatedWorld.getBlock(packet.getBlockPosition()).getType() != StateTypes.RESPAWN_ANCHOR) return;
            if (held.is(ItemTypes.GLOWSTONE)) return; // charging, not detonating
            add(ticksSincePlace);
            ticksSincePlace = Integer.MAX_VALUE;
        } else if (isTickPacket(event.getPacketType()) && ticksSincePlace != Integer.MAX_VALUE) {
            ticksSincePlace++;
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

    /** Pure decision rule, unit-tested. */
    public static boolean isBotLike(@NotNull ClickStats stats, double maxMeanTicks, double maxStdDevTicks) {
        return stats.samples() > 0 && stats.meanTicks() <= maxMeanTicks && stats.stdDevTicks() <= maxStdDevTicks;
    }
}
