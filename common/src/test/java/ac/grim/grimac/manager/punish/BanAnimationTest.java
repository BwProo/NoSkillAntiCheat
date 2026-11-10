package ac.grim.grimac.manager.punish;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BanAnimationTest {
    @Test
    void prefixIsStripped() {
        assertEquals("ban %player% Cheating", BanAnimation.stripPrefix("[animation] ban %player% Cheating"));
        assertNull(BanAnimation.stripPrefix("ban %player% Cheating"));
    }

    @Test
    void helixRisesAndNarrows() {
        double[] start = BanAnimation.helixPoint(0, 0);
        double[] top = BanAnimation.helixPoint(BanAnimation.HELIX_FRAMES, 0);
        assertEquals(0.0, start[1], 1e-9);
        assertEquals(2.4, top[1], 1e-9);
        assertTrue(Math.hypot(top[0], top[2]) < Math.hypot(start[0], start[2]));
    }

    @Test
    void strandsAreOpposite() {
        for (int f = 0; f <= BanAnimation.HELIX_FRAMES; f += 7) {
            double[] a = BanAnimation.helixPoint(f, 0), b = BanAnimation.helixPoint(f, 1);
            assertEquals(-a[0], b[0], 1e-9);
            assertEquals(-a[2], b[2], 1e-9);
            assertEquals(a[1], b[1], 1e-9);
        }
    }

    @Test
    void strikeHappensAfterHelixAndBeforeEnd() {
        assertTrue(BanAnimation.STRIKE_FRAME > BanAnimation.HELIX_FRAMES);
        assertTrue(BanAnimation.END_FRAME > BanAnimation.STRIKE_FRAME + 12);
    }
}
