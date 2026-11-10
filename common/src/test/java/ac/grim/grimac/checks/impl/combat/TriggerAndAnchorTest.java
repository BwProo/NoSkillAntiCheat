package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.impl.autoclicker.ClickStats;
import ac.grim.grimac.checks.impl.misc.Improbable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerAndAnchorTest {

    // TriggerBot defaults: >= 85 % instant hits on crosshair entry and <= 10 % air swings
    @Test
    void triggerBotInstantAndNeverMissingIsFlagged() {
        assertTrue(TriggerBot.isBotLike(15, 15, 15, 0, 0.85, 0.1));
        assertTrue(TriggerBot.isBotLike(15, 13, 20, 1, 0.85, 0.1));
    }

    @Test
    void spamClickingHumanIsNotFlagged() {
        // instant hits happen, but half of the swings go into the air
        assertFalse(TriggerBot.isBotLike(15, 15, 60, 30, 0.85, 0.1));
    }

    @Test
    void timingHumanIsNotFlagged() {
        // hits land several ticks after reaching the target
        assertFalse(TriggerBot.isBotLike(15, 4, 15, 0, 0.85, 0.1));
    }

    @Test
    void triggerBotNeedsSamples() {
        assertFalse(TriggerBot.isBotLike(0, 0, 0, 0, 0.85, 0.1));
    }

    // AnchorTiming defaults: mean <= 2 ticks and std dev <= 1 over the window
    @Test
    void anchorAuraSpeedIsFlagged() {
        int[] gaps = {1, 2, 1, 1, 2, 1, 2, 1, 1, 2};
        assertTrue(AnchorTiming.isBotLike(ClickStats.of(gaps, gaps.length), 2.0, 1.0));
    }

    @Test
    void manualAnchorPvpIsNotFlagged() {
        // place, switch to glowstone, charge, switch away, detonate: 5-12 ticks by hand
        int[] gaps = {6, 8, 5, 11, 7, 9, 6, 12, 8, 7};
        assertFalse(AnchorTiming.isBotLike(ClickStats.of(gaps, gaps.length), 2.0, 1.0));
    }

    @Test
    void newChecksFeedImprobable() {
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("TriggerBot"));
        assertEquals(Improbable.Category.COMBAT, Improbable.categoryOf("AnchorTiming"));
    }
}
