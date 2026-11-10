package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

/**
 * NoSkillAntiCheat: NoJumpDelay. Vanilla waits 10 ticks between jumps while the jump key is held
 * (releasing the key resets the delay). Since 1.21.2 the client reports its held keys, so jumps
 * closer than 10 ticks apart with the key held the whole time are impossible without a cheat
 * (typically bunny-hopping in 2-block-high tunnels). Water, lava, ladders, vehicles and flight
 * are ignored; three such jumps in a row are needed.
 */
@CheckData(name = "NoJumpDelay", stableKey = "nsac.movement.no_jump_delay", description = "Jumped again before the vanilla jump delay while holding jump", experimental = true, decay = 0.2)
public class NoJumpDelay extends Check implements PostPredictionListener {
    private static final int VANILLA_DELAY_TICKS = 10;
    private long tick;
    private long lastJumpTick = Long.MIN_VALUE;
    private boolean releasedSinceLastJump = true;
    private int streak;

    public NoJumpDelay(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        tick++;
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2)) return;
        if (!player.packetStateData.knownInput.jump()) releasedSinceLastJump = true;
        if (!predictionComplete.isChecked() || !player.predictedVelocity.isJump()) return;
        if (player.wasTouchingWater || player.wasTouchingLava || player.isClimbing || player.inVehicle()
                || player.isFlying || player.isGliding) {
            lastJumpTick = tick;
            releasedSinceLastJump = false;
            return;
        }
        long gap = lastJumpTick == Long.MIN_VALUE ? Long.MAX_VALUE : tick - lastJumpTick;
        if (violates(gap, releasedSinceLastJump)) {
            if (++streak >= 3) flag("jumped " + gap + " ticks after the last jump with jump held");
        } else {
            streak = 0;
            reward();
        }
        lastJumpTick = tick;
        releasedSinceLastJump = false;
    }

    /** Pure rule, unit-tested. */
    public static boolean violates(long ticksSinceLastJump, boolean releasedJumpInBetween) {
        return !releasedJumpInBetween && ticksSinceLastJump < VANILLA_DELAY_TICKS;
    }
}
