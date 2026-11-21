package ac.grim.grimac.utils.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PistonSupportTest {
    @Test
    void blockUnderTheFeetCounts() {
        assertTrue(PistonSupport.isUnderFeet(64.0, 64.0));   // standing on it
        assertTrue(PistonSupport.isUnderFeet(63.0, 64.0));   // the block below that
        assertTrue(PistonSupport.isUnderFeet(64.0, 64.05));  // slightly above, e.g. jumping off
    }

    @Test
    void blocksBesideOrAboveDoNot() {
        assertFalse(PistonSupport.isUnderFeet(65.0, 64.0));  // at body height: horizontal push only
        assertFalse(PistonSupport.isUnderFeet(62.0, 64.0));  // two blocks down
    }
}
