package ac.grim.grimac.checks.impl.misc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TranslationProbeTest {
    private final List<TranslationProbe.Key> keys = TranslationProbe.parse(TranslationProbe.DEFAULT_KEYS);

    @Test
    void defaultKeyIsMeteor() {
        assertEquals(1, keys.size());
        assertEquals("Meteor Client", keys.get(0).label());
        assertEquals("key.category.meteor-client.meteor-client", keys.get(0).key());
    }

    @Test
    void vanillaClientReturnsTheKeyItself() {
        assertNull(TranslationProbe.detect(keys, new String[]{"key.category.meteor-client.meteor-client", "", "", ""}));
    }

    @Test
    void clientWithMeteorReturnsTranslation() {
        assertEquals("Meteor Client", TranslationProbe.detect(keys, new String[]{"Meteor Client", "", "", ""}));
    }

    @Test
    void emptyOrMissingLinesAreNotADetection() {
        assertNull(TranslationProbe.detect(keys, new String[]{"", "", "", ""}));
        assertNull(TranslationProbe.detect(keys, null));
        assertNull(TranslationProbe.detect(keys, new String[0]));
    }

    @Test
    void parseSkipsBrokenEntriesAndKeepsAtMostFour() {
        List<TranslationProbe.Key> parsed = TranslationProbe.parse(List.of(
                "A=a.key", "broken", "=nokey", "B=", "C=c.key", "D=d.key", "E=e.key", "F=f.key"));
        assertEquals(List.of("A", "C", "D", "E"), parsed.stream().map(TranslationProbe.Key::label).toList());
    }
}
