package ac.grim.grimac.checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckNamesTest {

    @Test
    void knownChecksGetReadableNames() {
        assertEquals("Reach", CheckNames.friendly("Reach"));
        assertEquals("Velocity (anti-knockback)", CheckNames.friendly("AntiKB"));
        assertEquals("InvMove (click while moving)", CheckNames.friendly("MultiActionsC"));
        assertEquals("Hacked client", CheckNames.friendly("BlacklistedClient"));
    }

    @Test
    void familiesKeepTheirLetter() {
        assertEquals("BadPackets H", CheckNames.friendly("BadPacketsH"));
        assertEquals("ElytraFly C", CheckNames.friendly("ElytraC"));
        assertEquals("Packet order B", CheckNames.friendly("PacketOrderB"));
    }

    @Test
    void unknownChecksKeepTheirId() {
        assertEquals("SomethingNew", CheckNames.friendly("SomethingNew"));
        assertEquals("BadPackets", CheckNames.friendly("BadPackets"));
    }
}
