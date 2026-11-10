package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.impl.misc.Improbable;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalTimingTest {
    private static final double MEAN = 0.6, STD = 0.5; // defaults

    @Test
    void sameTickPlaceAndBreakEveryTimeIsBotLike() {
        int[] gaps = new int[20]; // all 0 ticks
        assertTrue(CrystalTiming.isBotLike(ClickStats.of(gaps, 20), MEAN, STD));
    }

    @Test
    void mostlyInstantWithRareOneTickIsStillBotLike() {
        int[] gaps = new int[20];
        gaps[5] = 1;
        gaps[13] = 1;
        assertTrue(CrystalTiming.isBotLike(ClickStats.of(gaps, 20), MEAN, STD));
    }

    @Test
    void fastHumanCrystalPvpIsNotFlagged() {
        // a very fast human: 1-4 ticks between the place click and the break click
        Random random = new Random(3);
        int[] gaps = new int[20];
        for (int i = 0; i < gaps.length; i++) gaps[i] = 1 + random.nextInt(4);
        assertFalse(CrystalTiming.isBotLike(ClickStats.of(gaps, 20), MEAN, STD));
    }

    @Test
    void constantButSlowGapIsNotFlaggedByThisCheck() {
        int[] gaps = new int[20];
        java.util.Arrays.fill(gaps, 3); // regular, but a human-possible speed; AutoClicker checks cover regularity
        assertFalse(CrystalTiming.isBotLike(ClickStats.of(gaps, 20), MEAN, STD));
    }

    @Test
    void crystalChecksFeedImprobable() {
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("CrystalTiming"));
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("InvalidInteractTarget"));
    }
}
