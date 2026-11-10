package ac.grim.grimac.checks.impl.combat;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillAuraAimTest {

    @Test
    void lookingStraightAtTargetCenterHasZeroOffset() {
        // target 3 blocks south (+z) at the same height: yaw 0; eye 1.62 vs center 0.9 -> look down a bit
        double pitchToCenter = -Math.toDegrees(Math.atan2(0.9 - 1.62, 3));
        double[] o = KillAuraAim.offsets(0, pitchToCenter, 0, 0, 3);
        assertEquals(0, o[0], 1e-9);
        assertEquals(0, o[1], 1e-9);
    }

    @Test
    void yawOffsetWrapsAround() {
        // target to the east (-x is yaw 90 in Minecraft, +x is yaw -90); looking yaw 270 == -90
        double[] o = KillAuraAim.offsets(270, 0, 3, 0.72, 0);
        assertEquals(0, o[0], 1e-9);
    }

    @Test
    void botLockingOntoSamePointIsFlagged() {
        assertTrue(KillAuraAim.isAimLock(0.2, 0.3, 0.5, 1.0, 5.0));
    }

    @Test
    void humanHittingAllOverTheHitboxIsNotFlagged() {
        Random random = new Random(5);
        double[] yaw = new double[15], pitch = new double[15];
        for (int i = 0; i < 15; i++) {
            yaw[i] = random.nextGaussian() * 4;
            pitch[i] = random.nextGaussian() * 5;
        }
        assertFalse(KillAuraAim.isAimLock(KillAuraAim.std(yaw), KillAuraAim.std(pitch), 3, 1.0, 5.0));
    }

    @Test
    void aimLockBesideTheTargetIsNotThisCheck() {
        // consistent but far off to the side: not a kill aura aim lock (Hitboxes/Reach handle misses)
        assertFalse(KillAuraAim.isAimLock(0.2, 0.2, 20, 1.0, 5.0));
    }
}
