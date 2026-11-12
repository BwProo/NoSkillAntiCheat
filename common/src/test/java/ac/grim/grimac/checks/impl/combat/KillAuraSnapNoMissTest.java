package ac.grim.grimac.checks.impl.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillAuraSnapNoMissTest {
    @Test
    void lookingStraightAtTheTargetCenterIsZeroError() {
        // target 3 blocks south (+z), body centre at eye height minus 0.72
        double pitch = -Math.toDegrees(Math.atan2(0.9 - 1.62, 3));
        assertEquals(0, KillAuraSnap.aimError(0, pitch, 0, 0, 3), 1e-6);
        assertTrue(KillAuraSnap.aimError(90, pitch, 0, 0, 3) > 80); // looking west, target south
    }

    @Test
    void snapNeedsToComeFromFarAndCloseIn() {
        assertTrue(KillAuraSnap.isSnap(61, 9));      // Meteor: 61 deg off, hit at 9 deg
        assertFalse(KillAuraSnap.isSnap(25, 2));     // was nearly on target already
        assertFalse(KillAuraSnap.isSnap(45, 35));    // turned a bit, still far
    }

    @Test
    void anyMissClearsTheNoMissWindow() {
        assertTrue(KillAuraNoMiss.isAura(40, 0));
        assertFalse(KillAuraNoMiss.isAura(40, 1));
        assertFalse(KillAuraNoMiss.isAura(0, 0));
    }
}
