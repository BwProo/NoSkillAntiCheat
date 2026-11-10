package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (idea from public Vulcan aim check descriptions): during combat, yaw speed that
 * changes too smoothly over many consecutive ticks, typical for interpolated (bot) aim.
 */
@CheckData(name = "AimSmooth", stableKey = "nsac.aim.smooth", description = "Combat rotations were unnaturally smooth", experimental = true, decay = 0.25)
public class AimSmooth extends CombatAimCheck {
    private float[] deltas;
    private int count;
    private double minSmoothness, minYawSpeed;

    public AimSmooth(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        int size = Math.max(10, config.getIntElse(getConfigName() + ".sample-size", 30));
        if (deltas == null || deltas.length != size) {
            deltas = new float[size];
            count = 0;
        }
        minSmoothness = config.getDoubleElse(getConfigName() + ".min-smoothness", 0.08);
        minYawSpeed = config.getDoubleElse(getConfigName() + ".min-yaw-speed", 1.0);
    }

    @Override
    protected void processCombatRotation(@NotNull RotationUpdate rotation) {
        float deltaYaw = rotation.deltaYaw();
        if (Math.abs(deltaYaw) < minYawSpeed || Math.abs(deltaYaw) > 60f) {
            count = 0; // streak of steady turning broken
            return;
        }
        deltas[count++] = deltaYaw;
        if (count == deltas.length) {
            double smoothness = AimMath.smoothness(deltas, count);
            count = 0;
            if (!Double.isNaN(smoothness) && smoothness < minSmoothness) {
                flag(String.format(Locale.ROOT, "smoothness=%.3f samples=%d", smoothness, deltas.length));
            } else {
                reward();
            }
        }
    }

    @Override
    protected void onExempt() {
        count = 0;
    }
}
