package ac.grim.grimac.manager.stats;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * NoSkillAntiCheat: lightweight false-positive measurement.
 *
 * <p>Counts flags, setbacks and "shadow" flags (experimental checks that are not active for the
 * player, so they never punish) per check, plus how many player-hours were observed. While you play
 * legit, every counted flag is a false positive. Hot path cost: one map lookup + one LongAdder
 * increment per flag; nothing is stored per packet.
 */
public final class FlagStats implements StartableInitable {
    public static final FlagStats INSTANCE = new FlagStats();
    private static final long SAMPLE_SECONDS = 10;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static final class Counter {
        public final LongAdder flags = new LongAdder();
        public final LongAdder setbacks = new LongAdder();
        public final LongAdder shadowFlags = new LongAdder();
        public volatile boolean experimental;
    }

    public static final class Window {
        public final @NotNull String label;
        public final long startedAt = System.currentTimeMillis();
        public final AtomicLong playerMillis = new AtomicLong();
        public final Map<String, Counter> checks = new ConcurrentHashMap<>();

        Window(@NotNull String label) {
            this.label = label;
        }

        public double playerHours() {
            return playerMillis.get() / 3_600_000.0;
        }
    }

    private final Window total = new Window("total");
    private volatile @Nullable Window session;
    private boolean started;

    private FlagStats() {}

    @Override
    public synchronized void start() {
        if (started) return;
        started = true;
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            long millis = GrimAPI.INSTANCE.getPlayerDataManager().size() * SAMPLE_SECONDS * 1000L;
            total.playerMillis.addAndGet(millis);
            Window current = session;
            if (current != null) current.playerMillis.addAndGet(millis);
        }, SAMPLE_SECONDS, SAMPLE_SECONDS, TimeUnit.SECONDS);
    }

    // --- hooks called from Check -------------------------------------------------------------

    public static void flag(@NotNull Check check) {
        INSTANCE.counters(check).forEach(c -> c.flags.increment());
    }

    public static void setback(@NotNull Check check) {
        INSTANCE.counters(check).forEach(c -> c.setbacks.increment());
    }

    public static void shadow(@NotNull Check check) {
        INSTANCE.counters(check).forEach(c -> c.shadowFlags.increment());
    }

    private List<Counter> counters(@NotNull Check check) {
        List<Counter> list = new ArrayList<>(2);
        list.add(counter(total, check));
        Window current = session;
        if (current != null) list.add(counter(current, check));
        return list;
    }

    private static Counter counter(Window window, Check check) {
        Counter counter = window.checks.computeIfAbsent(check.getCheckName(), k -> new Counter());
        counter.experimental = check.isExperimental();
        return counter;
    }

    // --- sessions ------------------------------------------------------------------------------

    public @NotNull Window total() {
        return total;
    }

    public @Nullable Window session() {
        return session;
    }

    public synchronized @Nullable Window startSession(@NotNull String label) {
        Window previous = session;
        session = new Window(label);
        return previous;
    }

    /** Ends the running session and writes its report. Returns the report file, or null if none ran. */
    public synchronized @Nullable File stopSession() throws IOException {
        Window current = session;
        if (current == null) return null;
        session = null;
        return writeReport(current);
    }

    public @NotNull File writeReport(@NotNull Window window) throws IOException {
        File folder = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "sessions");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Could not create " + folder);
        String time = LocalDateTime.now().format(FILE_TIME);
        File file = new File(folder, time + "-" + window.label + ".md");
        Files.writeString(file.toPath(), toMarkdown(window, time), StandardCharsets.UTF_8);
        LogUtil.info("Session report written to " + file.getPath());
        return file;
    }

    public static @NotNull String toMarkdown(@NotNull Window window, @NotNull String time) {
        double hours = window.playerHours();
        StringBuilder sb = new StringBuilder();
        sb.append("# Session `").append(window.label).append("` (").append(time).append(")\n\n");
        sb.append(String.format(Locale.ROOT, "Duration: %.1f min, player-hours: %.2f%n%n",
                (System.currentTimeMillis() - window.startedAt) / 60000.0, hours));
        sb.append("If this was legit play, every flag below is a false positive.\n\n");
        sb.append("| Check | Flags | Flags per player-hour | Setbacks | Shadow flags (experimental, inactive) |\n");
        sb.append("|---|---|---|---|---|\n");
        for (Map.Entry<String, Counter> e : sortedChecks(window)) {
            Counter c = e.getValue();
            sb.append("| ").append(e.getKey()).append(c.experimental ? " (exp.)" : "")
                    .append(" | ").append(c.flags.sum())
                    .append(" | ").append(hours > 0 ? String.format(Locale.ROOT, "%.1f", c.flags.sum() / hours) : "-")
                    .append(" | ").append(c.setbacks.sum())
                    .append(" | ").append(c.shadowFlags.sum()).append(" |\n");
        }
        if (window.checks.isEmpty()) sb.append("| (no flags) | 0 | 0 | 0 | 0 |\n");
        return sb.toString();
    }

    public static @NotNull List<Map.Entry<String, Counter>> sortedChecks(@NotNull Window window) {
        List<Map.Entry<String, Counter>> list = new ArrayList<>(window.checks.entrySet());
        list.sort((a, b) -> Long.compare(
                b.getValue().flags.sum() + b.getValue().shadowFlags.sum(),
                a.getValue().flags.sum() + a.getValue().shadowFlags.sum()));
        return list;
    }
}
