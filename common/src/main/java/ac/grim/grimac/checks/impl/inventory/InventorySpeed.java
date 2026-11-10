package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat: inventory clicks in every single tick for too long (ChestStealer, AutoArmor,
 * inventory macros, Mouse Tweaks drags). Vanilla's shift+double-click moves many stacks, but in
 * one tick, so it never builds a streak. Drag-crafting (QUICK_CRAFT) and double-click pickup are
 * ignored.
 */
@CheckData(name = "InventorySpeed", stableKey = "nsac.inventory.speed", description = "Clicked in an inventory in too many consecutive ticks", experimental = true, decay = 0.1)
public class InventorySpeed extends Check implements PacketReceiveListener {
    private int clicksThisTick;
    private int streakTicks, streakClicks;
    private int maxStreakTicks;

    public InventorySpeed(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        maxStreakTicks = Math.max(4, config.getIntElse(getConfigName() + ".max-consecutive-ticks", 8));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow.WindowClickType type = new WrapperPlayClientClickWindow(event).getWindowClickType();
            if (counts(type)) clicksThisTick++;
        } else if (isTickPacket(event.getPacketType())) {
            if (clicksThisTick > 0) {
                streakTicks++;
                streakClicks += clicksThisTick;
                if (streakTicks == maxStreakTicks + 1) {
                    flag(String.format(Locale.ROOT, "%d clicks in %d consecutive ticks", streakClicks, streakTicks));
                }
            } else {
                if (streakTicks > 0 && streakTicks <= maxStreakTicks) reward();
                streakTicks = streakClicks = 0;
            }
            clicksThisTick = 0;
        }
    }

    /** Pure rule, unit-tested: which click types count. */
    public static boolean counts(@NotNull WrapperPlayClientClickWindow.WindowClickType type) {
        return switch (type) {
            case PICKUP, QUICK_MOVE, SWAP, CLONE, THROW -> true;
            default -> false; // QUICK_CRAFT (drag), PICKUP_ALL (double click), UNKNOWN
        };
    }
}
