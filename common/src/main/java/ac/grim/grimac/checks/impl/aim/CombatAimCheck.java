package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.RotationListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: base for aim heuristics that only look at rotations shortly after the player
 * attacked something ("in combat"), and skip teleports, vehicles and forced rotations.
 */
public abstract class CombatAimCheck extends Check implements RotationListener, PacketReceiveListener {
    protected static final int COMBAT_TICKS = 40; // two seconds after an attack
    private int ticksSinceAttack = Integer.MAX_VALUE;

    protected CombatAimCheck(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (ac.grim.grimac.utils.anticheat.AttackPackets.attackedPlayer(player, event) != ac.grim.grimac.utils.anticheat.AttackPackets.NONE) {
            ticksSinceAttack = 0;
        } else if (isTickPacket(event.getPacketType()) && ticksSinceAttack != Integer.MAX_VALUE) {
            ticksSinceAttack++;
        }
    }

    @Override
    public final void process(final RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.compensatedEntities.self.getRiding() != null) {
            onExempt();
            return;
        }
        if (ticksSinceAttack > COMBAT_TICKS) {
            onExempt();
            return;
        }
        processCombatRotation(rotationUpdate);
    }

    /** Rotation while in combat and not exempt. */
    protected abstract void processCombatRotation(@NotNull RotationUpdate rotationUpdate);

    /** Called for rotations that are skipped; subclasses may reset streaks here. */
    protected void onExempt() {}
}
