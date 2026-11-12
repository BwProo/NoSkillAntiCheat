package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (idea from public Vulcan kill aura descriptions): switches between different
 * targets on consecutive attacks within a very short time, again and again ("switch aura").
 * Same-tick multi-target hits are already covered by MultiInteractA.
 */
@CheckData(name = "KillAuraSwitch", stableKey = "nsac.killaura.switch", description = "Switched attack targets unnaturally fast and often", experimental = true, decay = 0.25)
public class KillAuraSwitch extends Check implements PacketReceiveListener {
    private int lastTarget = Integer.MIN_VALUE;
    private int ticksSinceAttack = Integer.MAX_VALUE;
    private int attacks, fastSwitches;
    private int windowSize, maxSwitchTicks, maxFastSwitches;

    public KillAuraSwitch(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        windowSize = Math.max(5, config.getIntElse(getConfigName() + ".sample-size", 20));
        maxSwitchTicks = config.getIntElse(getConfigName() + ".max-switch-ticks", 2);
        maxFastSwitches = config.getIntElse(getConfigName() + ".max-fast-switches", 8);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int attacked = ac.grim.grimac.utils.anticheat.AttackPackets.attackedPlayer(player, event);
        if (attacked != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
            int target = attacked;
            if (lastTarget != Integer.MIN_VALUE && target != lastTarget && ticksSinceAttack <= maxSwitchTicks) {
                fastSwitches++;
            }
            lastTarget = target;
            ticksSinceAttack = 0;
            if (++attacks >= windowSize) {
                if (fastSwitches > maxFastSwitches) {
                    flag(String.format(Locale.ROOT, "fastSwitches=%d attacks=%d", fastSwitches, attacks));
                } else {
                    reward();
                }
                attacks = fastSwitches = 0;
            }
        } else if (isTickPacket(event.getPacketType()) && ticksSinceAttack != Integer.MAX_VALUE) {
            ticksSinceAttack++;
        }
    }
}
