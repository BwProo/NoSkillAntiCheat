package ac.grim.grimac.checks.impl.prediction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightContextTest {

    @Test
    void stayingUpInTheAirIsAFlightTick() {
        assertTrue(FlightContext.isFlightTick(false, 0.08)); // physics says fall, player keeps height
    }

    @Test
    void groundOrNormalFallingIsNot() {
        assertFalse(FlightContext.isFlightTick(true, 0.5));
        assertFalse(FlightContext.isFlightTick(false, 0.01));
        assertFalse(FlightContext.isFlightTick(false, -0.2));
    }
}
