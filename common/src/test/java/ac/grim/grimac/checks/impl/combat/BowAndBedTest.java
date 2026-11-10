package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BowAndBedTest {
    @Test
    void identicalShortDrawsAreBowSpam() {
        int[] draws = {4, 4, 4, 4, 4, 4, 4, 4};
        assertTrue(BowSpam.isBotLike(ClickStats.of(draws, draws.length), 6.0, 0.5));
    }

    @Test
    void humanBowTappingVaries() {
        int[] draws = {4, 7, 5, 9, 3, 6, 8, 5};
        assertFalse(BowSpam.isBotLike(ClickStats.of(draws, draws.length), 6.0, 0.5));
    }

    @Test
    void fullyChargedShotsAreFine() {
        int[] draws = {20, 20, 20, 20, 20, 20, 20, 20}; // consistent, but a full draw
        assertFalse(BowSpam.isBotLike(ClickStats.of(draws, draws.length), 6.0, 0.5));
    }

    @Test
    void recognisesBedItems() {
        assertTrue(BedTiming.isBed("red_bed"));
        assertTrue(BedTiming.isBed("light_blue_bed"));
        assertFalse(BedTiming.isBed("bedrock"));
    }
}
