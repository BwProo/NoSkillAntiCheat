package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.RotationListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.KnownInput;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Nullable;

/**
 * NoSkillAntiCheat: moving, jumping or looking around while an inventory is open (InvMove cheats).
 * Vanilla releases all movement keys when a screen opens and the mouse does not turn the camera.
 * An inventory click proves the screen is open; since 1.21.2 the client reports its pressed keys,
 * so a click while movement keys are held, or right after a rotation, is suspicious. Three such
 * clicks in a row are needed, so a click in the very tick the screen opens or closes is ignored.
 */
@CheckData(name = "InventoryMove", stableKey = "nsac.inventory.move", description = "Moved or looked around while an inventory was open", experimental = true, decay = 0.1)
public class InventoryMove extends Check implements PacketReceiveListener, RotationListener {
    private static final int REQUIRED_STREAK = 3;
    private int ticksSinceRotation = Integer.MAX_VALUE;
    private int streak;

    public InventoryMove(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotation) {
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.horseInteractCausedForcedRotation) return;
        if (rotation.deltaYawABS() + rotation.deltaPitchABS() > 0.5f) ticksSinceRotation = 0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            boolean inputKnown = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
            String reason = reason(inputKnown ? player.packetStateData.knownInput : null, ticksSinceRotation <= 1);
            if (reason == null) {
                streak = 0;
                reward();
            } else if (++streak >= REQUIRED_STREAK) {
                flag(reason);
            }
        } else if (isTickPacket(event.getPacketType()) && ticksSinceRotation != Integer.MAX_VALUE) {
            ticksSinceRotation++;
        }
    }

    /** Pure rule, unit-tested: why this inventory click is suspicious, or null. */
    public static @Nullable String reason(@Nullable KnownInput input, boolean rotatedJustNow) {
        if (input != null && input.moving()) return "moving keys held while clicking in inventory";
        if (rotatedJustNow) return "camera turned while clicking in inventory";
        return null;
    }
}
