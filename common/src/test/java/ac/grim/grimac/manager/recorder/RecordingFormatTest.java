package ac.grim.grimac.manager.recorder;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingFormatTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void pseudonymIsStablePerSaltAndHidesTheUuid() {
        String a = RecordingFormat.pseudonym("salt-1", PLAYER);
        assertEquals(a, RecordingFormat.pseudonym("salt-1", PLAYER));
        assertNotEquals(a, RecordingFormat.pseudonym("salt-2", PLAYER));
        assertEquals(16, a.length());
        assertFalse(a.contains("0000000000"));
    }

    @Test
    void rowHasOneValuePerHeaderColumn() {
        int columns = RecordingFormat.HEADER.split(",", -1).length;
        StringBuilder withTarget = new StringBuilder();
        RecordingFormat.appendRow(withTarget, 5, 90f, 10f, 1.5f, -0.25f, 1, 64, -3, true, 2, true,
                42, 0.5, 0, 2.5, true, 35, 0.5);
        StringBuilder withoutTarget = new StringBuilder();
        RecordingFormat.appendRow(withoutTarget, 6, 90f, 10f, 0f, 0f, 1, 64, -3, false, 0, false,
                0, 0, 0, 0, false, 35, 0.5);
        assertEquals(columns, withTarget.toString().trim().split(",", -1).length);
        assertEquals(columns, withoutTarget.toString().trim().split(",", -1).length);
        assertTrue(withTarget.toString().startsWith("5,90.00000,10.00000,1.50000,-0.25000,"));
        assertTrue(withoutTarget.toString().contains(",0,0,,,,,35,"));
    }

    @Test
    void metadataEndsWithHeader() {
        String meta = RecordingFormat.metadata("legit-pvp", "abc", 775, 1L);
        assertTrue(meta.contains("# label=legit-pvp\n"));
        assertTrue(meta.endsWith(RecordingFormat.HEADER + "\n"));
    }

    @Test
    void labelsAreRestricted() {
        assertTrue(RecordingFormat.LABEL.matcher("cheat-killaura_1").matches());
        assertFalse(RecordingFormat.LABEL.matcher("../etc").matches());
        assertFalse(RecordingFormat.LABEL.matcher("").matches());
    }
}
