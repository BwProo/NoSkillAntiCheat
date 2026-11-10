package ac.grim.grimac.checks.impl.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickSwapTest {

    private static SwapPattern start(int slot) {
        SwapPattern p = new SwapPattern();
        p.slotChanged(slot);
        p.endTick();
        return p;
    }

    @Test
    void switchActAndBackInOneTickIsBot() {
        SwapPattern p = start(0);
        p.slotChanged(4); // axe
        p.action();       // hit the shield
        p.slotChanged(0); // back to sword
        assertEquals(2, p.endTick());
    }

    @Test
    void switchBackNextTickCountsHalf() {
        SwapPattern p = start(0);
        p.slotChanged(4);
        p.action();
        assertEquals(0, p.endTick());
        p.slotChanged(0);
        assertEquals(1, p.endTick());
    }

    @Test
    void normalSwitchingIsFine() {
        SwapPattern p = start(0);
        p.slotChanged(4);
        assertEquals(0, p.endTick());
        p.action(); // acting later on the new slot
        assertEquals(0, p.endTick());
        p.slotChanged(0);
        assertEquals(0, p.endTick());
    }

    @Test
    void actingBeforeTheSwitchDoesNotCount() {
        SwapPattern p = start(0);
        p.action();
        p.slotChanged(4);
        p.slotChanged(0);
        assertEquals(0, p.endTick());
    }
}
