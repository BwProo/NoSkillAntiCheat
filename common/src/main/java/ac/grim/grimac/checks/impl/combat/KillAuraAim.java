package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat: kill aura that aims for you (Meteor KillAura and similar, single target).
 * At every attack it measures where the crosshair is relative to the direction to the target's
 * center (yaw and pitch offset). A bot aims at the same point of the target every time, so both
 * offsets barely vary across many hits while the target moves; humans hit all over the hitbox.
 */
@CheckData(name = "KillAuraAim", stableKey = "nsac.killaura.aim", description = "Hit the target at the same aim point every time (aim lock)", experimental = true, decay = 0.25)
public class KillAuraAim extends Check implements PacketReceiveListener {
    private double[] yawOffsets, pitchOffsets;
    private int count;
    private double maxStdDev, maxMeanYawOffset, minDistance;

    public KillAuraAim(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(8, Math.min(100, config.getIntElse(getConfigName() + ".sample-size", 15)));
        if (yawOffsets == null || yawOffsets.length != size) {
            yawOffsets = new double[size];
            pitchOffsets = new double[size];
            count = 0;
        }
        maxStdDev = config.getDoubleElse(getConfigName() + ".max-std-dev", 1.0);
        maxMeanYawOffset = config.getDoubleElse(getConfigName() + ".max-mean-yaw-offset", 5.0);
        minDistance = config.getDoubleElse(getConfigName() + ".min-distance", 1.5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int attacked = ac.grim.grimac.utils.anticheat.AttackPackets.attackedEntity(event);
        if (attacked == ac.grim.grimac.utils.anticheat.AttackPackets.NONE) return;
        PacketEntity entity = player.compensatedEntities.getEntity(attacked);
        if (entity == null || entity.getType() == EntityTypes.END_CRYSTAL || !entity.isLivingEntity) return;
        if (player.compensatedEntities.self.getRiding() != null) return;

        Vector3d pos = entity.trackedServerPosition.getPos();
        double dx = pos.getX() - player.x, dy = pos.getY() - player.y, dz = pos.getZ() - player.z;
        if (Math.sqrt(dx * dx + dz * dz) < minDistance) return; // very close: any aim hits, angles explode

        double[] offset = offsets(player.yaw, player.pitch, dx, dy, dz);
        yawOffsets[count] = offset[0];
        pitchOffsets[count] = offset[1];
        if (++count < yawOffsets.length) return;
        count = 0;

        double yawStd = std(yawOffsets), pitchStd = std(pitchOffsets), meanYaw = meanAbs(yawOffsets);
        if (isAimLock(yawStd, pitchStd, meanYaw, maxStdDev, maxMeanYawOffset)) {
            flag(String.format(Locale.ROOT, "yawStd=%.2f pitchStd=%.2f meanYawOff=%.2f n=%d", yawStd, pitchStd, meanYaw, yawOffsets.length));
        } else {
            reward();
        }
    }

    /** Yaw and pitch offset (degrees) of the look direction from the direction to the target's center. */
    public static double[] offsets(double yaw, double pitch, double dx, double dy, double dz) {
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = -Math.toDegrees(Math.atan2(dy + 0.9 - 1.62, Math.sqrt(dx * dx + dz * dz)));
        double yawOffset = ((yaw - targetYaw) % 360 + 540) % 360 - 180;
        return new double[]{yawOffset, pitch - targetPitch};
    }

    /** Pure decision rule, unit-tested. */
    public static boolean isAimLock(double yawStd, double pitchStd, double meanAbsYawOffset, double maxStd, double maxMeanYaw) {
        return yawStd <= maxStd && pitchStd <= maxStd && meanAbsYawOffset <= maxMeanYaw;
    }

    static double std(double[] values) {
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.length;
        double sq = 0;
        for (double v : values) sq += (v - mean) * (v - mean);
        return Math.sqrt(sq / values.length);
    }

    private static double meanAbs(double[] values) {
        double sum = 0;
        for (double v : values) sum += Math.abs(v);
        return sum / values.length;
    }
}
