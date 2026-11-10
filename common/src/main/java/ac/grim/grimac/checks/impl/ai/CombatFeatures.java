package ac.grim.grimac.checks.impl.ai;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * NoSkillAntiCheat: combat features for the AI. MUST match tools/ai/features.py exactly; the
 * parity test CombatFeaturesParityTest compares both on a fixture. Change both together.
 */
public final class CombatFeatures {
    public static final int WINDOW_TICKS = 100;
    public static final int MIN_COMBAT_TICKS = 50;
    public static final List<String> NAMES = List.of(
            "mean_abs_dyaw", "std_dyaw", "mean_abs_dpitch", "std_dpitch",
            "yaw_smoothness", "yaw_jerk",
            "aim_err_mean", "aim_err_std", "on_target_frac", "aim_err_at_attack",
            "attacks_per_sec", "swings_per_sec", "miss_ratio", "click_gap_std",
            "ping_mean");
    private static final double ON_TARGET_DEGREES = 10.0;
    private static final double EYE_HEIGHT = 1.62;
    private static final double TARGET_CENTER = 0.9;

    private CombatFeatures() {}

    /** Angle in degrees between the look vector and eyes-to-target-center, or NaN if degenerate. */
    public static double aimErrorDegrees(double yaw, double pitch, double tdx, double tdy, double tdz) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double lx = -Math.sin(yr) * Math.cos(pr), ly = -Math.sin(pr), lz = Math.cos(yr) * Math.cos(pr);
        double tx = tdx, ty = tdy + TARGET_CENTER - EYE_HEIGHT, tz = tdz;
        double length = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (length < 1e-6) return Double.NaN;
        double dot = (lx * tx + ly * ty + lz * tz) / length;
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }

    public static double @NotNull [] compute(double[] yaw, double[] pitch, double[] dyaw, double[] dpitch,
                                             int[] swings, boolean[] attack, boolean[] hasTarget,
                                             double[] tdx, double[] tdy, double[] tdz, int[] ping, int n) {
        double meanAbsDyaw = 0, meanAbsDpitch = 0, meanDyaw = 0, meanDpitch = 0, meanPing = 0;
        for (int i = 0; i < n; i++) {
            meanAbsDyaw += Math.abs(dyaw[i]);
            meanAbsDpitch += Math.abs(dpitch[i]);
            meanDyaw += dyaw[i];
            meanDpitch += dpitch[i];
            meanPing += ping[i];
        }
        meanAbsDyaw /= n;
        meanAbsDpitch /= n;
        meanDyaw /= n;
        meanDpitch /= n;
        meanPing /= n;
        double varDyaw = 0, varDpitch = 0;
        for (int i = 0; i < n; i++) {
            varDyaw += (dyaw[i] - meanDyaw) * (dyaw[i] - meanDyaw);
            varDpitch += (dpitch[i] - meanDpitch) * (dpitch[i] - meanDpitch);
        }
        double stdDyaw = Math.sqrt(varDyaw / n), stdDpitch = Math.sqrt(varDpitch / n);

        double first = 0, second = 0;
        for (int i = 1; i < n; i++) first += Math.abs(dyaw[i] - dyaw[i - 1]);
        for (int i = 2; i < n; i++) second += Math.abs((dyaw[i] - dyaw[i - 1]) - (dyaw[i - 1] - dyaw[i - 2]));
        double yawSmoothness = (n > 1 ? first / (n - 1) : 0) / (meanAbsDyaw + 1e-6);
        double yawJerk = (n > 2 ? second / (n - 2) : 0) / (meanAbsDyaw + 1e-6);

        double errSum = 0, errSq = 0, attackErrSum = 0;
        int errCount = 0, onTarget = 0, attackErrCount = 0;
        double[] errors = new double[n];
        for (int i = 0; i < n; i++) {
            if (!hasTarget[i]) continue;
            double err = aimErrorDegrees(yaw[i], pitch[i], tdx[i], tdy[i], tdz[i]);
            if (Double.isNaN(err)) continue;
            errors[errCount++] = err;
            errSum += err;
            if (err < ON_TARGET_DEGREES) onTarget++;
            if (attack[i]) {
                attackErrSum += err;
                attackErrCount++;
            }
        }
        double errMean = errCount > 0 ? errSum / errCount : 0;
        for (int i = 0; i < errCount; i++) errSq += (errors[i] - errMean) * (errors[i] - errMean);
        double errStd = errCount > 0 ? Math.sqrt(errSq / errCount) : 0;

        double seconds = n / 20.0;
        int attacks = 0, swingSum = 0, swingTicks = 0, missed = 0, lastSwing = -1, gapCount = 0;
        double gapSum = 0;
        int[] gaps = new int[n];
        for (int i = 0; i < n; i++) {
            if (attack[i]) attacks++;
            swingSum += swings[i];
            if (swings[i] > 0) {
                swingTicks++;
                if (!attack[i]) missed++;
                if (lastSwing >= 0) {
                    gaps[gapCount++] = i - lastSwing;
                    gapSum += i - lastSwing;
                }
                lastSwing = i;
            }
        }
        double gapStd = 0;
        if (gapCount >= 2) {
            double gapMean = gapSum / gapCount, sq = 0;
            for (int i = 0; i < gapCount; i++) sq += (gaps[i] - gapMean) * (gaps[i] - gapMean);
            gapStd = Math.sqrt(sq / gapCount);
        }

        return new double[]{
                meanAbsDyaw, stdDyaw, meanAbsDpitch, stdDpitch,
                yawSmoothness, yawJerk,
                errMean, errStd, errCount > 0 ? onTarget / (double) errCount : 0, attackErrCount > 0 ? attackErrSum / attackErrCount : 0,
                attacks / seconds, swingSum / seconds, swingTicks > 0 ? missed / (double) swingTicks : 0, gapStd,
                meanPing / 1000.0
        };
    }
}
