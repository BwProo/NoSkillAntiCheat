package ac.grim.grimac.manager.protection;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** NoSkillAntiCheat: pure helpers for hiding the server software (used by BrandSpoofer). */
public final class ServerBrand {
    private static final Pattern SOFTWARE = Pattern.compile(
            "(?i)^\\s*(paper|purpur|spigot|folia|craftbukkit|pufferfish|leaf|velocity|waterfall|bungeecord)\\b\\s*");

    private ServerBrand() {}

    /** VarInt length + UTF-8, the format of the minecraft:brand payload. */
    public static byte @NotNull [] encodeBrand(@NotNull String brand) {
        byte[] text = brand.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length + 5);
        int value = text.length;
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
        out.write(text, 0, text.length);
        return out.toByteArray();
    }

    /** "Purpur 26.3" -> "26.3"; names without a known server software stay as they are. */
    public static @NotNull String cleanVersionName(@NotNull String name) {
        String cleaned = SOFTWARE.matcher(name).replaceFirst("").trim();
        return cleaned.isEmpty() ? name : cleaned;
    }
}
