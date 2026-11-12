package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalReactionTest {
    @Test
    void breakingInTheTickTheCrystalAppearsIsInstant() {
        int[] d = {0, 0, 1, 0, 0, 1, 0, 0, 0, 1};
        assertTrue(CrystalReaction.isInstant(ClickStats.of(d, d.length), 1.0, 0.6));
    }

    @Test
    void fastHumanReactionVaries() {
        int[] d = {2, 4, 1, 3, 5, 2, 3, 6, 2, 4}; // 50-300 ms
        assertFalse(CrystalReaction.isInstant(ClickStats.of(d, d.length), 1.0, 0.6));
    }
}
