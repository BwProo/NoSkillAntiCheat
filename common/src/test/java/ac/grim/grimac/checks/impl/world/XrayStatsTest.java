package ac.grim.grimac.checks.impl.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XrayStatsTest {

    @Test
    void xrayerFindsManyHiddenVeinsInLittleStone() {
        XrayStats s = new XrayStats(30 * 60_000);
        for (int vein = 0; vein < 5; vein++) {
            for (int i = 0; i < 30; i++) s.mined(vein * 10_000L + i);
            s.tunnelVein(vein * 10_000L + 50);
        }
        assertEquals(150, s.mined());
        assertTrue(s.suspicious(4, 100)); // 30 blocks per vein
    }

    @Test
    void legitBranchMiningIsNotSuspicious() {
        XrayStats s = new XrayStats(30 * 60_000);
        for (int vein = 0; vein < 5; vein++) {
            for (int i = 0; i < 400; i++) s.mined(vein * 60_000L + i);
            s.tunnelVein(vein * 60_000L + 500);
        }
        assertFalse(s.suspicious(4, 100)); // 400 blocks per vein
    }

    @Test
    void tooFewVeinsNeverFlag() {
        XrayStats s = new XrayStats(30 * 60_000);
        s.mined(0);
        s.tunnelVein(1);
        s.tunnelVein(2);
        assertFalse(s.suspicious(4, 100));
    }

    @Test
    void oldEventsLeaveTheWindow() {
        XrayStats s = new XrayStats(60_000);
        s.tunnelVein(0);
        s.mined(0);
        s.mined(120_000); // two minutes later: the first two are gone
        assertEquals(1, s.mined());
        assertEquals(0, s.tunnelVeins());
    }
}
