package ac.grim.grimac.manager.recorder;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * NoSkillAntiCheat: records one CSV row per client tick for a player while a recording runs
 * ({@code /nsac record start <player> <label>}). Training data for the AI and replay data for
 * regression tests.
 *
 * <p>Lightweight: does nothing unless recording. Rows are built on the player's netty thread and
 * handed off every {@value #FLUSH_ROWS} ticks (~10 s) to one shared background thread that appends a
 * gzip member to the file (concatenated gzip members form one valid .gz file). Players are stored
 * as a salted SHA-256 pseudonym, never by name or UUID.
 */
public final class SessionRecorder extends GrimProcessor implements PacketReceiveListener {
    private static final int FLUSH_ROWS = 200;
    private static final int TARGET_TICKS = 100;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "NoSkillAntiCheat-recorder");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static volatile String salt;

    private volatile @Nullable File file;
    private @Nullable String label;
    private long startedAt;
    private long maxMillis;
    private StringBuilder rows = new StringBuilder();
    private int rowCount;
    private long tick;
    private float lastYaw, lastPitch;
    private int swings;
    private boolean attack;
    private int target = Integer.MIN_VALUE;
    private int ticksSinceTarget = Integer.MAX_VALUE;

    public SessionRecorder(GrimPlayer player) {
        super(player);
    }

    public synchronized boolean isRecording() {
        return file != null;
    }

    public synchronized @Nullable String label() {
        return label;
    }

    /** Starts recording; returns the file, or throws if the folder cannot be prepared. */
    public synchronized @NotNull File start(@NotNull String newLabel, int maxMinutes) throws IOException {
        if (file != null) stopLocked();
        File folder = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "recordings");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Could not create " + folder);
        String pseudonym = RecordingFormat.pseudonym(salt(folder), player.getUniqueId());
        startedAt = System.currentTimeMillis();
        maxMillis = Math.max(1, maxMinutes) * 60_000L;
        file = new File(folder, LocalDateTime.now().format(FILE_TIME) + "-" + newLabel + "-" + pseudonym + ".csv.gz");
        label = newLabel;
        rows = new StringBuilder(RecordingFormat.metadata(newLabel, pseudonym,
                player.getClientVersion().getProtocolVersion(), startedAt));
        rowCount = 0;
        tick = 0;
        lastYaw = player.yaw;
        lastPitch = player.pitch;
        return file;
    }

    /** Stops recording and writes the rest; returns the file or null if none was running. */
    public synchronized @Nullable File stop() {
        return stopLocked();
    }

    private @Nullable File stopLocked() {
        File done = file;
        if (done == null) return null;
        flush(done, rows);
        file = null;
        label = null;
        rows = new StringBuilder();
        LogUtil.info("Recording saved: " + done.getPath());
        return done;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (file == null) return; // unsynchronized fast path; start/stop re-check under the lock
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                attack = true;
                target = packet.getEntityId();
                ticksSinceTarget = 0;
            }
        } else if (isAnimation(event.getPacketType())) {
            swings++;
        } else if (isTickPacket(event.getPacketType())) {
            onTick();
        }
    }

    private synchronized void onTick() {
        File current = file;
        if (current == null) return;
        if (System.currentTimeMillis() - startedAt > maxMillis) {
            LogUtil.info("Recording of " + player.getName() + " reached its time limit.");
            stopLocked();
            return;
        }

        boolean hasTarget = false;
        double tdx = 0, tdy = 0, tdz = 0;
        if (ticksSinceTarget <= TARGET_TICKS) {
            PacketEntity entity = player.compensatedEntities.getEntity(target);
            if (entity != null) {
                Vector3d pos = entity.trackedServerPosition.getPos();
                tdx = pos.getX() - player.x;
                tdy = pos.getY() - player.y;
                tdz = pos.getZ() - player.z;
                hasTarget = true;
            }
        }
        AimProcessor aim = player.checkManager == null ? null : player.checkManager.get(AimProcessor.class);
        double sensitivity = aim == null ? 0 : aim.sensitivityPitch;

        RecordingFormat.appendRow(rows, tick++, player.yaw, player.pitch, player.yaw - lastYaw, player.pitch - lastPitch,
                player.x, player.y, player.z, player.onGround, swings, attack,
                target, tdx, tdy, tdz, hasTarget, player.getTransactionPing(), sensitivity);
        lastYaw = player.yaw;
        lastPitch = player.pitch;
        swings = 0;
        attack = false;
        if (ticksSinceTarget != Integer.MAX_VALUE) ticksSinceTarget++;

        if (++rowCount >= FLUSH_ROWS) {
            flush(current, rows);
            rows = new StringBuilder();
            rowCount = 0;
        }
    }

    private static void flush(@NotNull File target, @NotNull StringBuilder data) {
        if (data.isEmpty()) return;
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        WRITER.execute(() -> {
            try (OutputStream out = new GZIPOutputStream(new FileOutputStream(target, true))) {
                out.write(bytes);
            } catch (IOException e) {
                LogUtil.error("Failed to write recording " + target.getPath(), e);
            }
        });
    }

    private static String salt(File folder) throws IOException {
        String current = salt;
        if (current != null) return current;
        synchronized (SessionRecorder.class) {
            if (salt != null) return salt;
            File saltFile = new File(folder, ".salt");
            if (saltFile.isFile()) {
                salt = Files.readString(saltFile.toPath(), StandardCharsets.UTF_8).trim();
            } else {
                byte[] random = new byte[16];
                new SecureRandom().nextBytes(random);
                salt = HexFormat.of().formatHex(random);
                Files.writeString(saltFile.toPath(), salt, StandardCharsets.UTF_8);
            }
            return salt;
        }
    }
}
