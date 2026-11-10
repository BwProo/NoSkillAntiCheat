package ac.grim.grimac.checks.impl.autoclicker;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickStatsTest {

    private static int[] constant(int value, int n) {
        int[] a = new int[n];
        java.util.Arrays.fill(a, value);
        return a;
    }

    @Test
    void perfectMacroHasZeroDeviationAndOneDistinctDelay() {
        // a click every 2 ticks = 10 CPS, perfectly regular
        ClickStats s = ClickStats.of(constant(2, 50), 50);
        assertEquals(10.0, s.cps(), 1e-9);
        assertEquals(0.0, s.stdDevTicks(), 1e-9);
        assertEquals(1, s.distinctDelays());
    }

    @Test
    void humanLikeClickingVariesEnoughToPassDefaults() {
        // jittered 1-4 tick delays around ~10 CPS, like a real player
        Random random = new Random(42);
        int[] delays = new int[50];
        for (int i = 0; i < delays.length; i++) delays[i] = 1 + random.nextInt(4);
        ClickStats s = ClickStats.of(delays, 50);
        assertTrue(s.cps() > 6 && s.cps() < 12, "cps " + s.cps());
        assertTrue(s.stdDevTicks() >= 0.45, "std " + s.stdDevTicks()); // AutoClickerB default
        assertTrue(s.distinctDelays() >= 3, "distinct " + s.distinctDelays()); // AutoClickerC default
    }

    @Test
    void sameTickClicksGiveHighCps() {
        int[] delays = new int[50];
        for (int i = 0; i < delays.length; i++) delays[i] = i % 2; // 0,1,0,1 = two clicks per tick
        ClickStats s = ClickStats.of(delays, 50);
        assertEquals(40.0, s.cps(), 1e-9);
        assertTrue(s.cps() > 22.0); // AutoClickerA default max
    }

    @Test
    void momentsMatchKnownValues() {
        // delays 1,2,3,4: mean 2.5, population variance 1.25, symmetric -> skew 0, excess kurtosis -1.36
        ClickStats s = ClickStats.of(new int[]{1, 2, 3, 4}, 4);
        assertEquals(2.5, s.meanTicks(), 1e-9);
        assertEquals(Math.sqrt(1.25), s.stdDevTicks(), 1e-9);
        assertEquals(0.0, s.skewness(), 1e-9);
        assertEquals(-1.36, s.kurtosis(), 1e-9);
        assertEquals(4, s.distinctDelays());
    }

    @Test
    void holdingLeftClickWhileMiningIsNotCounted() {
        assertTrue(AbstractClickCheck.isMiningSwing(false, true, 0));   // breaking a block
        assertTrue(AbstractClickCheck.isMiningSwing(false, false, 3));  // pause right after a block broke
        assertTrue(AbstractClickCheck.isMiningSwing(false, false, 10));
        assertFalse(AbstractClickCheck.isMiningSwing(false, false, 11)); // back to fighting
        assertFalse(AbstractClickCheck.isMiningSwing(false, false, Integer.MAX_VALUE));
        assertTrue(AbstractClickCheck.isMiningSwing(true, false, Integer.MAX_VALUE)); // placing/using
    }

    @Test
    void emptyWindowIsSafe() {
        ClickStats s = ClickStats.of(new int[0], 0);
        assertEquals(0, s.samples());
        assertEquals(0.0, s.cps(), 1e-9);
    }
}
