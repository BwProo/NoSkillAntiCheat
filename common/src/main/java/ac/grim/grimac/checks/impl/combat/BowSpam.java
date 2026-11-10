package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat: BowSpam / Quiver / FastBow. Measures how many client ticks a bow was drawn
 * before release. Cheats release at a fixed short charge, shot after shot; players tapping the
 * bow by hand vary by several ticks. Flags windows of short draws that are nearly identical.
 */
@CheckData(name = "BowSpam", stableKey = "nsac.combat.bow_spam", description = "Released the bow after the same short draw time again and again", experimental = true, decay = 0.25)
public class BowSpam extends Check implements PacketReceiveListener {
    private int drawTicks = -1; // -1 = not drawing
    private int[] draws;
    private int count;
    private double maxMeanTicks, maxStdDevTicks;

    public BowSpam(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(4, Math.min(100, config.getIntElse(getConfigName() + ".sample-size", 8)));
        if (draws == null || draws.length != size) {
            draws = new int[size];
            count = 0;
        }
        maxMeanTicks = config.getDoubleElse(getConfigName() + ".max-mean-ticks", 6.0);
        maxStdDevTicks = config.getDoubleElse(getConfigName() + ".max-std-dev", 0.5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            if (player.inventory.getItemInHand(packet.getHand()).is(ItemTypes.BOW)) drawTicks = 0;
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            if (drawTicks < 0) return;
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() != DiggingAction.RELEASE_USE_ITEM) return;
            if (drawTicks >= 3) add(drawTicks); // below 3 ticks no arrow leaves the bow
            drawTicks = -1;
        } else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            drawTicks = -1;
        } else if (isTickPacket(event.getPacketType()) && drawTicks >= 0) {
            if (++drawTicks > 200) drawTicks = -1;
        }
    }

    private void add(int draw) {
        draws[count++] = draw;
        if (count < draws.length) return;
        ClickStats stats = ClickStats.of(draws, count);
        count = 0;
        if (isBotLike(stats, maxMeanTicks, maxStdDevTicks)) {
            flag(String.format(Locale.ROOT, "meanDraw=%.2f ticks std=%.2f n=%d", stats.meanTicks(), stats.stdDevTicks(), stats.samples()));
        } else {
            reward();
        }
    }

    /** Pure rule, unit-tested. */
    public static boolean isBotLike(@NotNull ClickStats stats, double maxMeanTicks, double maxStdDevTicks) {
        return stats.samples() > 0 && stats.meanTicks() <= maxMeanTicks && stats.stdDevTicks() <= maxStdDevTicks;
    }
}
