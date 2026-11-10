package ac.grim.grimac.checks.impl.autoclicker;

import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: statistics over a window of click delays, measured in client ticks
 * (0 = another click in the same tick). Pure and allocation-free apart from the result,
 * so it is cheap and unit-testable.
 */
public record ClickStats(int samples, double meanTicks, double cps, double stdDevTicks,
                         double skewness, double kurtosis, int distinctDelays) {

    /** Maximum delay that still counts as continuous clicking (longer pauses split the window). */
    public static final int MAX_DELAY_TICKS = 10;

    public static @NotNull ClickStats of(int @NotNull [] delays, int count) {
        if (count <= 0) return new ClickStats(0, 0, 0, 0, 0, 0, 0);
        double sum = 0;
        boolean[] seen = new boolean[MAX_DELAY_TICKS + 1];
        int distinct = 0;
        for (int i = 0; i < count; i++) {
            int d = delays[i];
            sum += d;
            int idx = Math.min(Math.max(d, 0), MAX_DELAY_TICKS);
            if (!seen[idx]) {
                seen[idx] = true;
                distinct++;
            }
        }
        double mean = sum / count;
        double m2 = 0, m3 = 0, m4 = 0;
        for (int i = 0; i < count; i++) {
            double dev = delays[i] - mean;
            double sq = dev * dev;
            m2 += sq;
            m3 += sq * dev;
            m4 += sq * sq;
        }
        m2 /= count;
        m3 /= count;
        m4 /= count;
        double std = Math.sqrt(m2);
        double skew = m2 > 0 ? m3 / Math.pow(m2, 1.5) : 0;
        double kurt = m2 > 0 ? m4 / (m2 * m2) - 3.0 : 0; // excess kurtosis
        // 20 ticks per second; a mean of 0 means all clicks in one tick, cap to avoid infinity
        double cps = 20.0 / Math.max(mean, 0.05);
        return new ClickStats(count, mean, cps, std, skew, kurt, distinct);
    }
}
