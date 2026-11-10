package ac.grim.grimac.checks.impl.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoSignTest {
    @Test
    void instantTextIsFlagged() {
        assertTrue(AutoSign.isTooFast(0, 12));
        assertTrue(AutoSign.isTooFast(3, 1));
    }

    @Test
    void typingOrClosingEmptyIsFine() {
        assertFalse(AutoSign.isTooFast(4, 12));   // a fast typist still needs more than 150 ms
        assertFalse(AutoSign.isTooFast(0, 0));    // pressing Escape right away sends empty lines
    }

    @Test
    void textLengthIgnoresBlanksAndNulls() {
        assertEquals(5, AutoSign.textLength(new String[]{" ab ", "", null, "cde"}));
        assertEquals(0, AutoSign.textLength(null));
    }

    @Test
    void recognisesSignItems() {
        assertTrue(AutoSign.isSign("oak_sign"));
        assertTrue(AutoSign.isSign("cherry_hanging_sign"));
        assertFalse(AutoSign.isSign("oak_planks"));
    }
}
