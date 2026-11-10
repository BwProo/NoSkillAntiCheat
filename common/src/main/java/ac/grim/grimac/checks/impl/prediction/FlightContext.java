package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

/**
 * NoSkillAntiCheat: remembers whether the player recently stayed higher in the air than the
 * prediction allows. After the first Flight flags Grim sets the player back, and the following
 * offsets no longer look like "rising in the air" on their own; with this context they are still
 * reported as Flight instead of Movement/Speed/GroundSpoof. Only affects the alert name.
 */
public final class FlightContext extends GrimProcessor implements PostPredictionListener {
    private static final int MEMORY_TICKS = 40;
    private long tick;
    private long lastFlightTick = Long.MIN_VALUE;

    public FlightContext(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        tick++;
        if (player.isFlying || player.canFly || player.isGliding || player.inVehicle()) return;
        double dy = player.actualMovement.getY() - player.predictedVelocity.vector.getY();
        if (isFlightTick(player.onGround, dy)) lastFlightTick = tick;
    }

    /** Pure rule, unit-tested: in the air and higher than the prediction allows. */
    public static boolean isFlightTick(boolean serverOnGround, double dy) {
        return !serverOnGround && dy > 0.03;
    }

    public boolean recentlyFlying() {
        return lastFlightTick != Long.MIN_VALUE && tick - lastFlightTick <= MEMORY_TICKS;
    }
}
