package ac.grim.grimac.checks.impl.aim;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AimMathTest {

    @Test
    void exactSensitivityStepsAreOnGrid() {
        double divisor = 0.15; // one mouse step in degrees
        assertEquals(0.0, AimMath.gridError(0.15 * 7, divisor), 1e-9);
        assertEquals(0.0, AimMath.gridError(-0.15 * 12, divisor), 1e-9); // sign does not matter
    }

    @Test
    void computedRotationIsOffGrid() {
        double divisor = 0.15;
        // 1.0 degree = 6.67 steps -> 0.33 away from the nearest step
        assertEquals(1.0 / 3.0, AimMath.gridError(1.0, divisor), 1e-9);
        assertTrue(AimMath.gridError(1.0, divisor) > 0.15); // AimGCD default max-grid-error
    }

    @Test
    void unusableDivisorGivesNaN() {
        assertTrue(Double.isNaN(AimMath.gridError(1.0, 0)));
        assertTrue(Double.isNaN(AimMath.gridError(1.0, -1)));
    }

    @Test
    void linearlyInterpolatedAimIsSmooth() {
        // yaw speed ramping 5.0, 5.1, 5.2 ... like a bot easing onto a target
        float[] deltas = new float[30];
        for (int i = 0; i < deltas.length; i++) deltas[i] = 5.0f + 0.1f * i;
        double s = AimMath.smoothness(deltas, deltas.length);
        assertTrue(s < 0.08, "smoothness " + s); // AimSmooth default min-smoothness
    }

    @Test
    void handMovementIsJerky() {
        Random random = new Random(7);
        float[] deltas = new float[30];
        for (int i = 0; i < deltas.length; i++) deltas[i] = 5.0f + (float) random.nextGaussian() * 2.0f;
        double s = AimMath.smoothness(deltas, deltas.length);
        assertTrue(s > 0.08, "smoothness " + s);
    }

    @Test
    void smoothnessNeedsEnoughSamples() {
        assertTrue(Double.isNaN(AimMath.smoothness(new float[]{1, 2}, 2)));
        assertTrue(Double.isNaN(AimMath.smoothness(new float[]{0, 0, 0}, 3)));
    }
}
