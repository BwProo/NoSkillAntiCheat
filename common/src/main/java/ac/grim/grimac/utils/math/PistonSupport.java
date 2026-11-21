package ac.grim.grimac.utils.math;

/** NoSkillAntiCheat: pure helpers for blocks moved by pistons. */
public final class PistonSupport {
    private PistonSupport() {
    }

    /**
     * True when a moving block whose top is at {@code boxMaxY} is the one the player stands on (or the
     * one just below it), so the client may keep standing on it while we only see a moving_piston.
     */
    public static boolean isUnderFeet(double boxMaxY, double feetY) {
        return boxMaxY <= feetY + 0.1 && boxMaxY >= feetY - 1.1;
    }
}
