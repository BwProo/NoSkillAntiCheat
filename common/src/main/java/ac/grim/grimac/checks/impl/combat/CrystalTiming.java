package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (idea from crystal-PvP cheats such as AutoCrystal / CrystalAura / FastCrystal):
 * measures the ticks between placing an end crystal and attacking an end crystal. A human needs two
 * separate clicks and varies the gap; automated crystal breaking is consistently near-instant.
 *
 * <p>Related existing checks: Reach covers crystal reach, InvalidInteractTarget covers attacking
 * entity IDs the client cannot know yet ("ID predict").
 */
@CheckData(name = "CrystalTiming", stableKey = "nsac.crystal.timing", description = "Broke end crystals right after placing them too consistently", experimental = true, decay = 0.25)
public class CrystalTiming extends Check implements PacketReceiveListener {
    private static final int MAX_GAP_TICKS = 10;
    private int ticksSincePlace = Integer.MAX_VALUE;
    private int[] delays;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public CrystalTiming(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(10, Math.min(200, config.getIntElse(getConfigName() + ".sample-size", 10)));
        if (delays == null || delays.length != size) {
            delays = new int[size];
            count = 0;
        }
        maxMeanTicks = config.getDoubleElse(getConfigName() + ".max-mean-ticks", 0.6);
        maxStdDevTicks = config.getDoubleElse(getConfigName() + ".max-std-dev", 0.5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            if (player.inventory.getItemInHand(packet.getHand()).is(ItemTypes.END_CRYSTAL)) {
                ticksSincePlace = 0;
            }
        } else if (ac.grim.grimac.utils.anticheat.AttackPackets.attackedEntity(event) != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
            PacketEntity entity = player.compensatedEntities.getEntity(ac.grim.grimac.utils.anticheat.AttackPackets.attackedEntity(event));
            if (entity == null || entity.getType() != EntityTypes.END_CRYSTAL) return;
            if (ticksSincePlace > MAX_GAP_TICKS) return; // not a place-then-break sequence
            add(ticksSincePlace);
            ticksSincePlace = Integer.MAX_VALUE; // one sample per placement
        } else if (isTickPacket(event.getPacketType()) && ticksSincePlace != Integer.MAX_VALUE) {
            ticksSincePlace++;
        }
    }

    private void add(int delay) {
        delays[count++] = delay;
        if (count < delays.length) return;
        ClickStats stats = ClickStats.of(delays, count);
        count = 0;
        if (isBotLike(stats, maxMeanTicks, maxStdDevTicks)) {
            flag(String.format(Locale.ROOT, "meanGap=%.2f ticks std=%.2f n=%d", stats.meanTicks(), stats.stdDevTicks(), stats.samples()));
        } else {
            reward();
        }
    }

    /** Pure decision rule, unit-tested: near-instant and nearly constant place-to-break gaps. */
    public static boolean isBotLike(@NotNull ClickStats stats, double maxMeanTicks, double maxStdDevTicks) {
        return stats.samples() > 0 && stats.meanTicks() <= maxMeanTicks && stats.stdDevTicks() <= maxStdDevTicks;
    }
}
