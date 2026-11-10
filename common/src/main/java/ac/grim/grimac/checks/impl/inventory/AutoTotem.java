package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;

import java.util.Locale;

/**
 * NoSkillAntiCheat: AutoTotem. Measures the client ticks between the client seeing its totem pop
 * (entity status 35, timed with a transaction so ping does not count) and the next move of an
 * item into the offhand (inventory click on the offhand slot, offhand swap button, or the F key).
 * By hand this takes several ticks; AutoTotem does it in 0-2 ticks every time.
 */
@CheckData(name = "AutoTotem", stableKey = "nsac.inventory.autototem", description = "Put a new totem into the offhand too fast after a totem pop", experimental = true, decay = 0.25)
public class AutoTotem extends Check implements PacketSendListener, PacketReceiveListener {
    private static final int TOTEM_POP = 35, OFFHAND_SLOT = 45, OFFHAND_SWAP_BUTTON = 40, MAX_WAIT_TICKS = 40, SAMPLES = 4;
    private volatile long popTick = -1;
    private long ticks;
    private final int[] gaps = new int[SAMPLES];
    private int count;

    public AutoTotem(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_STATUS) return;
        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(event);
        if (packet.getEntityId() == player.entityID && packet.getStatus() == TOTEM_POP) {
            player.addRealTimeTaskNext(() -> popTick = ticks); // when the client has seen the pop
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        var type = event.getPacketType();
        if (isTickPacket(type)) {
            ticks++;
            if (popTick != -1 && ticks - popTick > MAX_WAIT_TICKS) popTick = -1;
            return;
        }
        if (popTick == -1) return;
        boolean toOffhand = false;
        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            toOffhand = click.getSlot() == OFFHAND_SLOT
                    || (click.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.SWAP && click.getButton() == OFFHAND_SWAP_BUTTON);
        } else if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            toOffhand = new WrapperPlayClientPlayerDigging(event).getAction() == DiggingAction.SWAP_ITEM_WITH_OFFHAND;
        }
        if (!toOffhand) return;
        gaps[count++] = (int) (ticks - popTick);
        popTick = -1;
        if (count < SAMPLES) return;
        count = 0;
        if (isBotLike(gaps)) {
            flag(String.format(Locale.ROOT, "refilled offhand %d/%d/%d/%d ticks after totem pops", gaps[0], gaps[1], gaps[2], gaps[3]));
        } else {
            reward();
        }
    }

    /** Pure rule, unit-tested: every refill within 2 ticks of the pop. */
    public static boolean isBotLike(int[] gaps) {
        for (int gap : gaps) if (gap > 2) return false;
        return gaps.length > 0;
    }
}
