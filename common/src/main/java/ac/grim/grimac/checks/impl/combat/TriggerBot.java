package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat: trigger bot heuristic. During combat it tracks, once per tick, whether the
 * crosshair is on the last attacked target (one ray against one box, so it stays cheap). A trigger
 * bot attacks in the very tick the crosshair reaches the target and almost never swings at air.
 * Humans either spam-click (many air swings) or time their hits (several ticks after reaching the
 * target), so both conditions together are required.
 */
@CheckData(name = "TriggerBot", stableKey = "nsac.combat.triggerbot", description = "Attacked exactly when the crosshair reached the target, without missing", experimental = true, decay = 0.25)
public class TriggerBot extends Check implements PacketReceiveListener {
    private static final int COMBAT_TICKS = 100;
    private static final double RAY_LENGTH = 4.5; // generous: an earlier "on target" only makes reactions look slower

    private int target = Integer.MIN_VALUE;
    private int ticksSinceAttack = Integer.MAX_VALUE;
    private boolean onTarget;
    private int ticksOnTarget = -1; // -1: crosshair not on target, or this entry already produced a sample
    private boolean swungThisTick, attackedThisTick;

    private int entries, instantEntries, combatSwings, missedSwings;
    private int windowSize;
    private double minInstantRatio, maxMissRatio;

    public TriggerBot(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        windowSize = Math.max(5, config.getIntElse(getConfigName() + ".sample-size", 15));
        minInstantRatio = config.getDoubleElse(getConfigName() + ".min-instant-ratio", 0.85);
        maxMissRatio = config.getDoubleElse(getConfigName() + ".max-miss-ratio", 0.1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int attacked = ac.grim.grimac.utils.anticheat.AttackPackets.attackedEntity(event);
        if (attacked != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
            attackedThisTick = true;
            if (attacked != target) {
                target = attacked;
                onTarget = false;
                ticksOnTarget = -1;
            } else if (ticksOnTarget >= 0) {
                entries++;
                if (ticksOnTarget <= 1) instantEntries++; // same or next tick after reaching the target
                ticksOnTarget = -1;
                evaluateWindow();
            }
            ticksSinceAttack = 0;
        } else if (isAnimation(event.getPacketType())) {
            swungThisTick = true;
        } else if (isTickPacket(event.getPacketType())) {
            onTick();
        }
    }

    private void onTick() {
        boolean inCombat = ticksSinceAttack <= COMBAT_TICKS;
        if (inCombat && swungThisTick) {
            combatSwings++;
            if (!attackedThisTick) missedSwings++;
        }
        swungThisTick = attackedThisTick = false;
        if (ticksSinceAttack != Integer.MAX_VALUE) ticksSinceAttack++;
        if (!inCombat || target == Integer.MIN_VALUE) return;

        boolean nowOnTarget = isLookingAtTarget();
        if (nowOnTarget && !onTarget) {
            ticksOnTarget = 0; // crosshair just reached the target
        } else if (nowOnTarget && ticksOnTarget >= 0) {
            ticksOnTarget++;
        } else if (!nowOnTarget) {
            ticksOnTarget = -1;
        }
        onTarget = nowOnTarget;
    }

    private boolean isLookingAtTarget() {
        PacketEntity entity = player.compensatedEntities.getEntity(target);
        if (entity == null || entity.getType() == EntityTypes.END_CRYSTAL) return false;
        SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
        if (box == null) return false;
        Vector3dm look = ReachUtils.getLook(player, player.yaw, player.pitch);
        for (double eye : player.getPossibleEyeHeights()) {
            Vector3d eyePos = new Vector3d(player.x, player.y + eye, player.z);
            Vector3d end = eyePos.add(look.getX() * RAY_LENGTH, look.getY() * RAY_LENGTH, look.getZ() * RAY_LENGTH);
            if (ReachUtils.isVecInside(box, eyePos) || ReachUtils.calculateIntercept(box, eyePos, end).first() != null) {
                return true;
            }
        }
        return false;
    }

    private void evaluateWindow() {
        if (entries < windowSize) return;
        if (isBotLike(entries, instantEntries, combatSwings, missedSwings, minInstantRatio, maxMissRatio)) {
            flag(String.format(Locale.ROOT, "instant=%d/%d missRatio=%.2f", instantEntries, entries,
                    combatSwings == 0 ? 0 : missedSwings / (double) combatSwings));
        } else {
            reward();
        }
        entries = instantEntries = combatSwings = missedSwings = 0;
    }

    /** Pure decision rule, unit-tested. */
    public static boolean isBotLike(int entries, int instantEntries, int combatSwings, int missedSwings,
                                    double minInstantRatio, double maxMissRatio) {
        if (entries <= 0) return false;
        double instantRatio = instantEntries / (double) entries;
        double missRatio = combatSwings == 0 ? 0 : missedSwings / (double) combatSwings;
        return instantRatio >= minInstantRatio && missRatio <= maxMissRatio;
    }
}
