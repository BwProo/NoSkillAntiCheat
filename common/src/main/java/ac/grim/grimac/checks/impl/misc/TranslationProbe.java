package ac.grim.grimac.checks.impl.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * NoSkillAntiCheat: pure logic of the sign translation probe. Each probe line holds a translation
 * key of a mod. A vanilla client does not know the key and sends the key itself back; a client
 * with the mod sends the translated text. Unit-tested.
 */
public final class TranslationProbe {
    /** label=translation.key; the Meteor key is the one shown in Meteor issue #5864. */
    public static final List<String> DEFAULT_KEYS = List.of(
            "Meteor Client=key.category.meteor-client.meteor-client");

    public record Key(@NotNull String label, @NotNull String key) {}

    private TranslationProbe() {}

    public static @NotNull List<Key> parse(@NotNull List<String> entries) {
        List<Key> keys = new ArrayList<>();
        for (String entry : entries) {
            int eq = entry.indexOf('=');
            if (eq <= 0 || eq == entry.length() - 1) continue;
            keys.add(new Key(entry.substring(0, eq).trim(), entry.substring(eq + 1).trim()));
            if (keys.size() == 4) break; // a sign has four lines
        }
        return keys;
    }

    /** Returns the label of the first mod whose key came back translated, or null if none did. */
    public static @Nullable String detect(@NotNull List<Key> keys, String @Nullable [] returnedLines) {
        if (returnedLines == null) return null;
        for (int i = 0; i < keys.size() && i < returnedLines.length; i++) {
            String line = returnedLines[i] == null ? "" : returnedLines[i].trim();
            Key key = keys.get(i);
            // Unknown key: the client shows the key itself. Empty: line not filled (e.g. old client).
            if (!line.isEmpty() && !line.equals(key.key())) return key.label();
        }
        return null;
    }
}
