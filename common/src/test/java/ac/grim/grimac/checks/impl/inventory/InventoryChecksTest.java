package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.utils.data.KnownInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InventoryChecksTest {
    private static final KnownInput WALKING = new KnownInput(true, false, false, false, false, false, false);
    private static final KnownInput JUMPING = new KnownInput(false, false, false, false, true, false, false);
    private static final KnownInput SNEAKING_ONLY = new KnownInput(false, false, false, false, false, true, false);

    @Test
    void movingOrJumpingWhileClickingIsSuspicious() {
        assertNotNull(InventoryMove.reason(WALKING, false));
        assertNotNull(InventoryMove.reason(JUMPING, false));
    }

    @Test
    void turningTheCameraWhileClickingIsSuspicious() {
        assertNotNull(InventoryMove.reason(KnownInput.DEFAULT, true));
        assertNotNull(InventoryMove.reason(null, true)); // old clients: only the rotation part
    }

    @Test
    void normalInventoryUseIsFine() {
        assertNull(InventoryMove.reason(KnownInput.DEFAULT, false));
        assertNull(InventoryMove.reason(SNEAKING_ONLY, false)); // shift-clicking holds shift, not movement
        assertNull(InventoryMove.reason(null, false));
    }
}
