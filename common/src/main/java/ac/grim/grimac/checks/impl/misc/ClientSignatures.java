package ac.grim.grimac.checks.impl.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * NoSkillAntiCheat: matches a client's brand and registered plugin channels against configurable
 * regex patterns (case-insensitive). Pure and unit-testable.
 */
public final class ClientSignatures {
    /** Default patterns: names of well-known hacked clients and Baritone. Kept specific on purpose. */
    public static final List<String> DEFAULT_PATTERNS = List.of(
            "meteor", "wurst", "liquidbounce", "aristois", "bleachhack", "thunderhack", "rusherhack", "baritone");

    private final List<Pattern> patterns = new ArrayList<>();
    private final List<String> invalid = new ArrayList<>();

    public ClientSignatures(@NotNull List<String> regexes) {
        for (String regex : regexes) {
            try {
                patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                invalid.add(regex);
            }
        }
    }

    public @NotNull List<String> invalidPatterns() {
        return invalid;
    }

    /** Returns the pattern that matches {@code value}, or null. */
    public @Nullable String match(@Nullable String value) {
        if (value == null || value.isEmpty()) return null;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).find()) return pattern.pattern();
        }
        return null;
    }

    /** Decodes a minecraft:brand payload (VarInt length + UTF-8; brands are short, so 1 length byte). */
    public static @NotNull String decodeBrand(byte @NotNull [] data) {
        if (data.length <= 1) return "";
        return new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
    }

    /** Decodes a minecraft:register payload: channel names separated by NUL bytes. */
    public static @NotNull List<String> decodeRegister(byte @NotNull [] data) {
        List<String> channels = new ArrayList<>();
        for (String part : new String(data, StandardCharsets.UTF_8).split("\0")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) channels.add(trimmed);
        }
        return channels;
    }
}
