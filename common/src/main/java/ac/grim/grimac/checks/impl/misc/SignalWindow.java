package ac.grim.grimac.checks.impl.misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * NoSkillAntiCheat: remembers which distinct checks flagged within a sliding time window.
 * Not thread-safe; each player's flags are processed on that player's netty thread.
 */
public final class SignalWindow {
    private final Map<String, Long> lastSeen = new HashMap<>();
    private long windowMillis;
    private int minDistinct;

    public SignalWindow(long windowMillis, int minDistinct) {
        configure(windowMillis, minDistinct);
    }

    public void configure(long windowMillis, int minDistinct) {
        this.windowMillis = windowMillis;
        this.minDistinct = Math.max(2, minDistinct);
    }

    /** Records a flag of {@code check}; returns true if enough distinct checks are now active. */
    public boolean add(@NotNull String check, long now) {
        prune(now);
        lastSeen.put(check, now);
        return lastSeen.size() >= minDistinct;
    }

    public @NotNull List<String> active() {
        List<String> list = new ArrayList<>(lastSeen.keySet());
        list.sort(String::compareTo);
        return list;
    }

    public void clear() {
        lastSeen.clear();
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> it = lastSeen.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > windowMillis) it.remove();
        }
    }
}
