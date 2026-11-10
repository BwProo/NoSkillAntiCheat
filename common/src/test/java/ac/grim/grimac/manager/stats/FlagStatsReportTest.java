package ac.grim.grimac.manager.stats;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagStatsReportTest {

    @Test
    void reportShowsFlagsPerPlayerHourSortedByCount() {
        FlagStats.Window window = new FlagStats.Window("legit");
        window.playerMillis.set(2 * 3_600_000L); // two player-hours

        FlagStats.Counter simulation = new FlagStats.Counter();
        simulation.flags.add(4);
        simulation.setbacks.add(3);
        window.checks.put("Simulation", simulation);

        FlagStats.Counter packetOrder = new FlagStats.Counter();
        packetOrder.experimental = true;
        packetOrder.shadowFlags.add(10);
        window.checks.put("PacketOrderA", packetOrder);

        List<Map.Entry<String, FlagStats.Counter>> sorted = FlagStats.sortedChecks(window);
        assertEquals("PacketOrderA", sorted.get(0).getKey());
        assertEquals("Simulation", sorted.get(1).getKey());

        String md = FlagStats.toMarkdown(window, "20261110-120000");
        assertTrue(md.contains("player-hours: 2.00"), md);
        assertTrue(md.contains("| Simulation | 4 | 2.0 | 3 | 0 |"), md);
        assertTrue(md.contains("| PacketOrderA (exp.) | 0 | 0.0 | 0 | 10 |"), md);
    }

    @Test
    void emptyWindowStillProducesTable() {
        String md = FlagStats.toMarkdown(new FlagStats.Window("empty"), "20261110-120000");
        assertTrue(md.contains("| (no flags) | 0 | 0 | 0 | 0 |"), md);
        assertTrue(md.contains("player-hours: 0.00"), md);
    }
}
