package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.AttackPackets;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat (measured on Meteor's KillAura): an aura only swings when it hits, so in a
 * fight every arm swing comes with an attack. Players miss: in the recorded fights human-style
 * bots missed over half their swings, Meteor 0 of 131. Counts swings while in combat (an attack
 * in the last 2 seconds, no block breaking) and flags a whole window without a single miss.
 */
@CheckData(name = "KillAuraNoMiss", stableKey = "nsac.killaura.no_miss", description = "Never missed a swing over a long fight", experimental = true, decay = 0.2)
public class KillAuraNoMiss extends Check implements PacketReceiveListener {
    private static final int COMBAT_TICKS = 40;
    private int ticksSinceAttack = Integer.MAX_VALUE;
    private int swingsThisTick, attacksThisTick;
    private boolean excludedThisTick;
    private int swings, misses, windowSwings;

    public KillAuraNoMiss(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        windowSwings = Math.max(15, config.getIntElse(getConfigName() + ".window-swings", 40));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        var type = event.getPacketType();
        int attacked = AttackPackets.attackedEntity(event);
        if (attacked != AttackPackets.NONE) {
            if (AttackPackets.attackedPlayer(player, event) == AttackPackets.NONE) {
                excludedThisTick = true; // crystals, mobs: only fights against players count
                return;
            }
            attacksThisTick++;
            ticksSinceAttack = 0;
        } else if (isAnimation(type)) {
            swingsThisTick++;
        } else if (type == PacketType.Play.Client.PLAYER_DIGGING || type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || type == PacketType.Play.Client.USE_ITEM || AttackPackets.isNonAttackInteract(event)) {
            excludedThisTick = true; // mining, placing or using also swings the arm
        } else if (isTickPacket(type)) {
            if (!excludedThisTick && ticksSinceAttack <= COMBAT_TICKS && swingsThisTick > 0) {
                swings += swingsThisTick;
                misses += Math.max(0, swingsThisTick - attacksThisTick);
                if (swings >= windowSwings) {
                    if (isAura(swings, misses)) {
                        flag("swings=" + swings + " misses=0");
                    } else {
                        reward();
                    }
                    swings = 0;
                    misses = 0;
                }
            }
            swingsThisTick = 0;
            attacksThisTick = 0;
            excludedThisTick = false;
            if (ticksSinceAttack != Integer.MAX_VALUE) ticksSinceAttack++;
        }
    }

    /** Pure rule, unit-tested. */
    public static boolean isAura(int swings, int misses) {
        return swings > 0 && misses == 0;
    }
}
