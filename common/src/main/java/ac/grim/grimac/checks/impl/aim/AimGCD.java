package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.GrimMath;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * NoSkillAntiCheat (idea from public Vulcan aim check descriptions): during combat, pitch changes
 * that do not fit the player's own sensitivity grid. Real mouse input moves in multiples of a
 * sensitivity-dependent step; computed aim (aim assist, kill aura rotations) usually does not.
 */
@CheckData(name = "AimGCD", stableKey = "nsac.aim.gcd", description = "Combat rotations did not match the mouse sensitivity grid", experimental = true, decay = 0.25)
public class AimGCD extends CombatAimCheck {
    private int samples, offGrid;
    private int windowSize;
    private double maxGridError, maxOffGridRatio;

    public AimGCD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        windowSize = Math.max(10, config.getIntElse(getConfigName() + ".sample-size", 40));
        maxGridError = config.getDoubleElse(getConfigName() + ".max-grid-error", 0.15);
        maxOffGridRatio = config.getDoubleElse(getConfigName() + ".max-off-grid-ratio", 0.6);
    }

    @Override
    protected void processCombatRotation(@NotNull RotationUpdate rotation) {
        AimProcessor processor = player.checkManager.get(AimProcessor.class);
        if (processor == null || processor.modePitch <= GrimMath.MINIMUM_DIVISOR) return; // sensitivity unknown yet
        float deltaPitch = rotation.deltaPitchABS();
        if (deltaPitch < 0.5f || deltaPitch > 10f) return; // tiny or huge moves say little about the grid

        double error = AimMath.gridError(deltaPitch, processor.modePitch);
        if (Double.isNaN(error)) return;
        samples++;
        if (error > maxGridError) offGrid++;

        if (samples >= windowSize) {
            double ratio = offGrid / (double) samples;
            if (ratio > maxOffGridRatio) {
                flag(String.format(Locale.ROOT, "offGrid=%.2f samples=%d sens=%.0f%%", ratio, samples, processor.sensitivityPitch * 200));
            } else {
                reward();
            }
            samples = offGrid = 0;
        }
    }
}
