package ac.grim.grimac.utils.anticheat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuraItemsTest {
    @Test
    void namesTheAuraBehindAPlacement() {
        assertEquals("CrystalAura", AuraItems.forUse("end_crystal", "obsidian"));
        assertEquals("AnchorAura", AuraItems.forUse("respawn_anchor", "stone"));
        assertEquals("AnchorAura", AuraItems.forUse("glowstone", "respawn_anchor")); // charging
        assertEquals("BedAura", AuraItems.forUse("red_bed", "stone"));
        assertNull(AuraItems.forUse("cobblestone", "stone"));                       // plain scaffold
        assertNull(AuraItems.forUse(null, null));
    }

    @Test
    void crystalsAreTheOnlyAuraEntity() {
        assertEquals("CrystalAura", AuraItems.forEntity("end_crystal"));
        assertNull(AuraItems.forEntity("zombie"));
    }
}
