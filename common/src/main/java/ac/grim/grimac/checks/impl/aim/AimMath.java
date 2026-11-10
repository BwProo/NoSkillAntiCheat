package ac.grim.grimac.checks.impl.aim;

/** NoSkillAntiCheat: pure helpers for the aim heuristics, kept separate so they are unit-testable. */
public final class AimMath {
    private AimMath() {}

    /**
     * How far a rotation delta is from the player's sensitivity grid: 0 means an exact multiple of
     * the divisor (a real mouse step), 0.5 is the worst case. Returns NaN if the divisor is unusable.
     */
    public static double gridError(double delta, double divisor) {
        if (!(divisor > 0) || Double.isNaN(delta)) return Double.NaN;
        double ratio = Math.abs(delta) / divisor;
        return Math.abs(ratio - Math.rint(ratio));
    }

    /**
     * Mean absolute change between consecutive deltas divided by the mean absolute delta.
     * Hand-moved mice are jerky (high value); interpolated aim is smooth (value near 0).
     * Returns NaN for fewer than 3 samples or no movement.
     */
    public static double smoothness(float[] deltas, int count) {
        if (count < 3) return Double.NaN;
        double sumAbs = 0, sumChange = 0;
        for (int i = 0; i < count; i++) {
            sumAbs += Math.abs(deltas[i]);
            if (i > 0) sumChange += Math.abs(deltas[i] - deltas[i - 1]);
        }
        double meanAbs = sumAbs / count;
        if (meanAbs == 0) return Double.NaN;
        return (sumChange / (count - 1)) / meanAbs;
    }
}
