package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

/**
 * NoSkillAntiCheat: switch to another hotbar slot, attack/use/place, and switch straight back
 * (ShieldBreaker, MaceSwap, KeybindPearl, AutoTool and similar). Doing all three inside one tick is
 * impossible by hand; switching back one tick later counts half. Two "points" within the window flag.
 */
@CheckData(name = "QuickSwap", stableKey = "nsac.inventory.quickswap", description = "Switched slot, acted and switched back faster than humanly possible", experimental = true, decay = 0.25)
public class QuickSwap extends Check implements PacketReceiveListener {
    private String lastAura; // NoSkillAntiCheat: aura item used in the current window, for the alert name
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
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY || type == PacketType.Play.Client.ATTACK) {
            int attacked = ac.grim.grimac.utils.anticheat.AttackPackets.attackedEntity(event);
            if (attacked != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
                pattern.action();
                var entity = player.compensatedEntities.getEntity(attacked);
                String aura = entity == null ? null : ac.grim.grimac.utils.anticheat.AuraItems.forEntity(entity.getType().getName().getKey());
                if (aura != null) lastAura = aura;
            }
        } else if (type == PacketType.Play.Client.USE_ITEM || type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            pattern.action();
            String aura = ac.grim.grimac.utils.anticheat.AuraItems.forUse(player.inventory.getHeldItem().getType().getName().getKey(), null);
            if (aura != null) lastAura = aura;
        } else if (isTickPacket(type)) {
            int result = pattern.endTick();
            if (ticksSinceFirst != Integer.MAX_VALUE && ++ticksSinceFirst > WINDOW_TICKS) {
                points = 0;
                ticksSinceFirst = Integer.MAX_VALUE;
                lastAura = null;
            }
            if (result > 0) {
                if (points == 0) ticksSinceFirst = 0;
                points += result;
                if (points >= 4) {
                    if (lastAura != null) flagAs(lastAura + " (auto switch)");
                    else clearFlagAs();
                    flag(result == 2 ? "switch, act and switch back in one tick" : "switch back right after acting");
                    points = 0;
                    ticksSinceFirst = Integer.MAX_VALUE;
                }
            }
        }
    }
}
