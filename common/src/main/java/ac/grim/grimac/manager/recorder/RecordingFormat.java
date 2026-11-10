package ac.grim.grimac.manager.recorder;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * NoSkillAntiCheat: file format of session recordings (gzip-compressed CSV, one row per client
 * tick). Pure helpers, unit-tested; the Python tools in tools/ai read the same columns.
 */
public final class RecordingFormat {
    public static final int VERSION = 1;
    public static final Pattern LABEL = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    public static final String HEADER = "tick,yaw,pitch,dyaw,dpitch,x,y,z,ground,swings,attack,target,tdx,tdy,tdz,ping,sens";

    private RecordingFormat() {}

    /** Stable pseudonym for a player: first 16 hex chars of SHA-256(salt + uuid). */
    public static @NotNull String pseudonym(@NotNull String salt, @NotNull UUID uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + uuid).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static @NotNull String metadata(@NotNull String label, @NotNull String pseudonym, int clientProtocol, long startedAt) {
        return "# nsac-recording v" + VERSION + "\n"
                + "# label=" + label + "\n"
                + "# player=" + pseudonym + "\n"
                + "# client_protocol=" + clientProtocol + "\n"
                + "# started_at=" + startedAt + "\n"
                + HEADER + "\n";
    }

    /** One CSV row. Target columns are empty when there is no recent target. */
    public static void appendRow(@NotNull StringBuilder sb, long tick, float yaw, float pitch, float dYaw, float dPitch,
                                 double x, double y, double z, boolean ground, int swings, boolean attack,
                                 int target, double tdx, double tdy, double tdz, boolean hasTarget, int ping, double sensitivity) {
        sb.append(tick).append(',')
                .append(f(yaw)).append(',').append(f(pitch)).append(',')
                .append(f(dYaw)).append(',').append(f(dPitch)).append(',')
                .append(f(x)).append(',').append(f(y)).append(',').append(f(z)).append(',')
                .append(ground ? 1 : 0).append(',').append(swings).append(',').append(attack ? 1 : 0).append(',');
        if (hasTarget) {
            sb.append(target).append(',').append(f(tdx)).append(',').append(f(tdy)).append(',').append(f(tdz));
        } else {
            sb.append(",,,");
        }
        sb.append(',').append(ping).append(',').append(f(sensitivity)).append('\n');
    }

    private static String f(double value) {
        return String.format(Locale.ROOT, "%.5f", value);
    }
}
