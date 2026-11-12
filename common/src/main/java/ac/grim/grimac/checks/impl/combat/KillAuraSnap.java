package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.AttackPackets;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat (measured on Meteor's KillAura): "silent" auras look somewhere else and only
 * turn onto the target in the tick they hit. Per client tick with an attack, compare the aim error
 * to the attacked entity with the rotation of the previous tick: at least 30 degrees off before and
 * at least 20 degrees closer now is a snap-hit. In the recorded Meteor fight 21 % of hits were
 * snap-hits; human-style bots had none. Flags when a window of attacks has too many.
 */
@CheckData(name = "KillAuraSnap", stableKey = "nsac.killaura.snap", description = "Turned onto the target only in the tick of the hit, again and again", experimental = true, decay = 0.25)
public class KillAuraSnap extends Check implements PacketReceiveListener {
    private int target = AttackPackets.NONE;
    private boolean attackedThisTick;
    private double lastYaw, lastPitch;
    private boolean hasLast;
    private int attacks, snaps;
    private int windowAttacks, maxSnaps;

    public KillAuraSnap(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        windowAttacks = Math.max(8, config.getIntElse(getConfigName() + ".window-attacks", 20));
        maxSnaps = Math.max(1, config.getIntElse(getConfigName() + ".max-snaps", 3));
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int attacked = AttackPackets.attackedEntity(event);
        if (attacked != AttackPackets.NONE) {
            target = attacked;
            attackedThisTick = true;
            return;
        }
        if (!isTickPacket(event.getPacketType())) return;
        if (attackedThisTick && hasLast && player.compensatedEntities.self.getRiding() == null) {
            PacketEntity entity = player.compensatedEntities.getEntity(target);
            if (entity != null && entity.isLivingEntity && entity.getType() != EntityTypes.END_CRYSTAL) {
                Vector3d pos = entity.trackedServerPosition.getPos();
                double dx = pos.getX() - player.x, dy = pos.getY() - player.y, dz = pos.getZ() - player.z;
                double before = aimError(lastYaw, lastPitch, dx, dy, dz);
                double now = aimError(player.yaw, player.pitch, dx, dy, dz);
                attacks++;
                if (isSnap(before, now)) snaps++;
                if (attacks >= windowAttacks) {
                    if (snaps > maxSnaps) {
                        flag("snap-hits=" + snaps + "/" + attacks);
                    } else {
                        reward();
                    }
                    attacks = 0;
                    snaps = 0;
                }
            }
        }
        attackedThisTick = false;
        lastYaw = player.yaw;
        lastPitch = player.pitch;
        hasLast = true;
    }

    /** Pure rule, unit-tested. */
    public static boolean isSnap(double errorBefore, double errorNow) {
        return errorBefore >= 30 && errorBefore - errorNow >= 20;
    }

    /** Angle (degrees) between the look vector and eyes-to-target-center; same formula as tools/ai/features.py. */
    public static double aimError(double yaw, double pitch, double dx, double dy, double dz) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double lx = -Math.sin(yr) * Math.cos(pr), ly = -Math.sin(pr), lz = Math.cos(yr) * Math.cos(pr);
        double ty = dy + 0.9 - 1.62;
        double length = Math.sqrt(dx * dx + ty * ty + dz * dz);
        if (length < 1e-6) return 0;
        double dot = (lx * dx + ly * ty + lz * dz) / length;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
    }
}
