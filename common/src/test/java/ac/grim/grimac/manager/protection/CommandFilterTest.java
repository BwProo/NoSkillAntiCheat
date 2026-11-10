package ac.grim.grimac.manager.protection;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandFilterTest {
    private final Set<String> blocked = new HashSet<>(CommandFilter.DEFAULT_BLOCKED);

    @Test
    void extractsLabel() {
        assertEquals("pl", CommandFilter.label("/pl"));
        assertEquals("bukkit:plugins", CommandFilter.label("/Bukkit:Plugins extra args"));
        assertEquals("?", CommandFilter.label("/? 2"));
        assertEquals("version", CommandFilter.label("  /VERSION NoSkillAntiCheat"));
    }

    @Test
    void blocksPluginListingCommands() {
        for (String cmd : new String[]{"pl", "plugins", "?", "help", "version", "ver", "about", "icanhasbukkit"}) {
            assertTrue(CommandFilter.isBlocked(cmd, blocked, false), cmd);
        }
    }

    @Test
    void blocksNamespacedVariants() {
        assertTrue(CommandFilter.isBlocked("bukkit:pl", blocked, false)); // base name is blocked anyway
        assertTrue(CommandFilter.isBlocked("minecraft:help", blocked, false));
        assertTrue(CommandFilter.isBlocked("essentials:spawn", blocked, true)); // all namespaced when enabled
        assertFalse(CommandFilter.isBlocked("essentials:spawn", blocked, false));
    }

    @Test
    void normalCommandsStayAllowed() {
        for (String cmd : new String[]{"spawn", "msg", "tpa", "home", "nsac"}) {
            assertFalse(CommandFilter.isBlocked(cmd, blocked, true), cmd);
        }
    }

    @Test
    void tabListHidesBlockedAndNamespaced() {
        assertTrue(CommandFilter.hideFromList("plugins", blocked, true));
        assertTrue(CommandFilter.hideFromList("worldedit:wand", blocked, true));
        assertFalse(CommandFilter.hideFromList("spawn", blocked, true));
    }
}
