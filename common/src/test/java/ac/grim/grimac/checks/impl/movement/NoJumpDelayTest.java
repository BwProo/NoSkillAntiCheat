package ac.grim.grimac.checks.impl.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoJumpDelayTest {

    @Test
    void holdingJumpAndHoppingFasterThanVanillaIsCheating() {
        assertTrue(NoJumpDelay.violates(3, false)); // tunnel bunny-hop every 3 ticks
        assertTrue(NoJumpDelay.violates(9, false));
    }

    @Test
    void vanillaDelayAndTappingAreFine() {
        assertFalse(NoJumpDelay.violates(10, false)); // exactly the vanilla delay
        assertFalse(NoJumpDelay.violates(12, false)); // normal jump on flat ground
        assertFalse(NoJumpDelay.violates(3, true));   // released and pressed again: allowed in vanilla
        assertFalse(NoJumpDelay.violates(Long.MAX_VALUE, false));
    }
}
