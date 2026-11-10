package ac.grim.grimac.checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckNamesTest {

    @Test
    void knownChecksGetReadableNames() {
        assertEquals("Reach (Reichweite)", CheckNames.friendly("Reach"));
        assertEquals("Velocity (AntiKnockback)", CheckNames.friendly("AntiKB"));
        assertEquals("InvMove (Klick beim Laufen)", CheckNames.friendly("MultiActionsC"));
        assertEquals("Cheat-Client", CheckNames.friendly("BlacklistedClient"));
    }

    @Test
    void familiesKeepTheirLetter() {
        assertEquals("BadPackets H", CheckNames.friendly("BadPacketsH"));
        assertEquals("ElytraFly C", CheckNames.friendly("ElytraC"));
        assertEquals("Paket-Reihenfolge B", CheckNames.friendly("PacketOrderB"));
    }

    @Test
    void unknownChecksKeepTheirId() {
        assertEquals("SomethingNew", CheckNames.friendly("SomethingNew"));
        assertEquals("BadPackets", CheckNames.friendly("BadPackets"));
    }
}
