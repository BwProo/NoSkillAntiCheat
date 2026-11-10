package ac.grim.grimac.checks.impl.misc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalWindowTest {

    @Test
    void oneNoisyCheckAloneNeverTriggers() {
        SignalWindow window = new SignalWindow(60_000, 3);
        for (int i = 0; i < 100; i++) {
            assertFalse(window.add("AimGCD", i * 100L));
        }
    }

    @Test
    void threeDistinctChecksWithinWindowTrigger() {
        SignalWindow window = new SignalWindow(60_000, 3);
        assertFalse(window.add("Reach", 0));
        assertFalse(window.add("AimGCD", 10_000));
        assertTrue(window.add("AutoClickerB", 20_000));
        assertEquals(List.of("AimGCD", "AutoClickerB", "Reach"), window.active());
    }

    @Test
    void oldSignalsExpire() {
        SignalWindow window = new SignalWindow(60_000, 3);
        window.add("Reach", 0);
        window.add("AimGCD", 1_000);
        // 70 s later the first two are outside the 60 s window
        assertFalse(window.add("AutoClickerB", 70_000));
        assertEquals(List.of("AutoClickerB"), window.active());
    }

    @Test
    void minimumIsAtLeastTwo() {
        SignalWindow window = new SignalWindow(60_000, 1);
        assertFalse(window.add("Reach", 0));
        assertTrue(window.add("Hitboxes", 1));
    }

    @Test
    void categoriesCoverNewAndCoreChecks() {
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("AimGCD"));
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("KillAuraSwitch"));
        assertEquals(Improbable.Category.MOVEMENT, Improbable.categoryOf("Simulation"));
        assertEquals(Improbable.Category.BUILD, Improbable.categoryOf("RotationPlace"));
        assertEquals(null, Improbable.categoryOf("ChatA"));
    }
}
