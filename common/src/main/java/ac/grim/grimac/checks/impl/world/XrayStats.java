package ac.grim.grimac.checks.impl.world;

import java.util.ArrayDeque;

/**
 * NoSkillAntiCheat: rolling mining statistics for the Xray check. Pure and unit-tested.
 * Counts natural underground blocks mined and rare-ore veins found by tunnelling (no cave next to
 * them) inside a time window.
 */
public final class XrayStats {
    private record Event(long time, boolean vein) {}

    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private final long windowMillis;
    private int mined, tunnelVeins;

    public XrayStats(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    public void mined(long now) {
        prune(now);
        events.addLast(new Event(now, false));
        mined++;
    }

    public void tunnelVein(long now) {
        prune(now);
        events.addLast(new Event(now, true));
        tunnelVeins++;
    }

    public int mined() {
        return mined;
    }

    public int tunnelVeins() {
        return tunnelVeins;
    }

    /** Blocks mined per tunnel-found rare vein (lower = more suspicious); MAX_VALUE without veins. */
    public double blocksPerVein() {
        return tunnelVeins == 0 ? Double.MAX_VALUE : mined / (double) tunnelVeins;
    }

    public boolean suspicious(int minVeins, double minBlocksPerVein) {
        return tunnelVeins >= minVeins && blocksPerVein() < minBlocksPerVein;
    }

    public void reset() {
        events.clear();
        mined = tunnelVeins = 0;
    }

    private void prune(long now) {
        while (!events.isEmpty() && now - events.peekFirst().time() > windowMillis) {
            if (events.pollFirst().vein()) tunnelVeins--;
            else mined--;
        }
    }
}
