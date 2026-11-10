package ac.grim.grimac.checks.impl.prediction;

import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: turns a Simulation offset into the cheat name staff expect in alerts.
 * Grim's Simulation only says "moved differently than predicted"; this looks at how the actual
 * movement differs from the predicted one. Pure and unit-tested.
 */
public final class MovementLabel {
    public static final String FLIGHT = "Flight";
    public static final String SPEED = "Speed";
    public static final String SIMULATION = "Simulation";
    private static final double MIN_DIFF = 0.03;

    private MovementLabel() {}

    /**
     * @param creativeFlying the player may fly (creative/spectator/allowed flight): never "Flight"
     */
    public static @NotNull String classify(double predX, double predY, double predZ,
                                           double actX, double actY, double actZ,
                                           boolean serverOnGround, boolean creativeFlying) {
        double dy = actY - predY;
        double dh = Math.hypot(actX, actZ) - Math.hypot(predX, predZ);
        if (!creativeFlying && !serverOnGround && dy > MIN_DIFF && dy >= dh) {
            return FLIGHT; // rising or hovering in the air more than physics allows
        }
        if (dh > MIN_DIFF && dh >= Math.abs(dy)) {
            return SPEED; // horizontally faster than predicted
        }
        return SIMULATION;
    }
}
