package ac.grim.grimac.manager.protection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperAntiXrayConfigTest {
    private static final String PAPER = String.join("\n",
            "_version: 31",
            "anticheat:",
            "  anti-xray:",
            "    enabled: false",
            "    engine-mode: 1",
            "    hidden-blocks:",
            "    - copper_ore",
            "    lava-obscures: false",
            "  obfuscation:",
            "    items:",
            "      enabled: false",
            "chunks:",
            "  enabled: false",
            "");

    @Test
    void enablesOnlyTheAntiXrayBlock() {
        String out = PaperAntiXrayConfig.enable(PAPER, 2);
        assertTrue(out.contains("  anti-xray:\n    enabled: true\n    engine-mode: 2\n"), out);
        assertTrue(out.contains("    items:\n      enabled: false"), out); // other blocks untouched
        assertTrue(out.contains("chunks:\n  enabled: false"), out);
        assertTrue(out.contains("    - copper_ore"), out);
    }

    @Test
    void alreadyEnabledStaysTheSame() {
        String once = PaperAntiXrayConfig.enable(PAPER, 2);
        assertEquals(once, PaperAntiXrayConfig.enable(once, 2));
    }
}
