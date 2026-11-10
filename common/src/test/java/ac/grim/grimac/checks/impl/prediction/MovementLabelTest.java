package ac.grim.grimac.checks.impl.prediction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementLabelTest {

    @Test
    void hoveringInAirIsFlight() {
        // physics says fall 0.08, player stays at the same height
        assertEquals("Flight", MovementLabel.classify(0, -0.08, 0, 0, 0, 0, false, false));
    }

    @Test
    void risingInAirIsFlight() {
        assertEquals("Flight", MovementLabel.classify(0.1, -0.08, 0, 0.1, 0.4, 0, false, false));
    }

    @Test
    void creativeFlightIsNeverFlight() {
        assertEquals("Simulation", MovementLabel.classify(0, -0.08, 0, 0, 0.2, 0, false, true));
    }

    @Test
    void tooFastOnGroundIsSpeed() {
        assertEquals("Speed", MovementLabel.classify(0.28, 0, 0, 0.6, 0, 0, true, false));
    }

    @Test
    void smallOrOtherDifferencesStaySimulation() {
        assertEquals("Simulation", MovementLabel.classify(0.2, -0.08, 0, 0.21, -0.07, 0, false, false));
        assertEquals("Simulation", MovementLabel.classify(0, -0.08, 0, 0, -0.5, 0, false, false)); // falling too fast
    }
}
