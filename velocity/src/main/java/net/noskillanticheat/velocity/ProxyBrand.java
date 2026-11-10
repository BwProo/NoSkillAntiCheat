package net.noskillanticheat.velocity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Pure helpers for hiding the proxy/server software. Unit-tested. */
public final class ProxyBrand {
    private static final Pattern SOFTWARE = Pattern.compile(
            "(?i)^\\s*(velocity|waterfall|bungeecord|paper|purpur|spigot|folia|craftbukkit|pufferfish|leaf)\\b\\s*");

    private ProxyBrand() {}

    /** VarInt length + UTF-8, the format of the minecraft:brand payload. */
    public static byte[] encodeBrand(String brand) {
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

    /** "Velocity 4.3.1" -> "4.3.1", "Paper 26.3" -> "26.3"; never returns an empty name. */
    public static String cleanVersionName(String name) {
        String cleaned = SOFTWARE.matcher(name).replaceFirst("").trim();
        return cleaned.isEmpty() ? name : cleaned;
    }
}
