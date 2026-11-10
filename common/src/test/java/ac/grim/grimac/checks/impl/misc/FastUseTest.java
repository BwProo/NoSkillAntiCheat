package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastUseTest {
    @Test
    void throwingEveryTickIsFlagged() {
        int[] gaps = new int[15];
        Arrays.fill(gaps, 1);
        assertTrue(FastUse.isBotLike(ClickStats.of(gaps, gaps.length), 1.25, 0.45));
    }

    @Test
    void holdingRightClickIsFine() {
        int[] gaps = new int[15];
        Arrays.fill(gaps, 4); // vanilla repeat delay
        assertFalse(FastUse.isBotLike(ClickStats.of(gaps, gaps.length), 1.25, 0.45));
    }

    @Test
    void fastHumanSpamClickingIsFine() {
        Random random = new Random(7);
        int[] gaps = new int[15];
        for (int i = 0; i < gaps.length; i++) gaps[i] = 1 + random.nextInt(3); // 1-3 ticks, ~10 CPS
        assertFalse(FastUse.isBotLike(ClickStats.of(gaps, gaps.length), 1.25, 0.45));
    }

    @Test
    void onlyThrowablesCount() {
        assertTrue(FastUse.isThrowable("experience_bottle"));
        assertTrue(FastUse.isThrowable("wind_charge"));
        assertFalse(FastUse.isThrowable("ender_pearl")); // has its own cooldown
        assertFalse(FastUse.isThrowable("golden_apple"));
    }
}
