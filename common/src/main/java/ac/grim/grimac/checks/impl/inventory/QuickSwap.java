package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

/**
 * NoSkillAntiCheat: switch to another hotbar slot, attack/use/place, and switch straight back
 * (ShieldBreaker, MaceSwap, KeybindPearl, AutoTool and similar). Doing all three inside one tick is
 * impossible by hand; switching back one tick later counts half. Two "points" within the window flag.
 */
@CheckData(name = "QuickSwap", stableKey = "nsac.inventory.quickswap", description = "Switched slot, acted and switched back faster than humanly possible", experimental = true, decay = 0.25)
public class QuickSwap extends Check implements PacketReceiveListener {
    private static final int WINDOW_TICKS = 600; // 30 s
    private final SwapPattern pattern = new SwapPattern();
    private int points, ticksSinceFirst = Integer.MAX_VALUE;

    public QuickSwap(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        var type = event.getPacketType();
        if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            pattern.slotChanged(new WrapperPlayClientHeldItemChange(event).getSlot());
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) pattern.action();
        } else if (type == PacketType.Play.Client.USE_ITEM || type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            pattern.action();
        } else if (isTickPacket(type)) {
            int result = pattern.endTick();
            if (ticksSinceFirst != Integer.MAX_VALUE && ++ticksSinceFirst > WINDOW_TICKS) {
                points = 0;
                ticksSinceFirst = Integer.MAX_VALUE;
            }
            if (result > 0) {
                if (points == 0) ticksSinceFirst = 0;
                points += result;
                if (points >= 4) {
                    flag(result == 2 ? "switch, act and switch back in one tick" : "switch back right after acting");
                    points = 0;
                    ticksSinceFirst = Integer.MAX_VALUE;
                }
            }
        }
    }
}
