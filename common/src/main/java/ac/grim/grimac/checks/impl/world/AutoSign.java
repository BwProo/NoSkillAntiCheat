package ac.grim.grimac.checks.impl.world;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.misc.SignProbe;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: AutoSign. After placing a sign the server opens the sign editor; AutoSign
 * modules answer with finished text before a human could type anything. The timer starts once
 * the client has acknowledged a transaction sent after the editor packet, so ping does not count.
 * Only freshly placed signs are checked: re-opening an existing sign shows its old text, which a
 * player may confirm instantly. NSAC's own sign probe is ignored.
 */
@CheckData(name = "AutoSign", stableKey = "nsac.world.auto_sign", description = "Wrote a placed sign faster than a human can type", experimental = true, decay = 0.25)
public class AutoSign extends Check implements PacketReceiveListener, PacketSendListener {
    private static final int MAX_TICKS_SINCE_PLACE = 40;
    private static final int MAX_TICKS_TO_TYPE = 3;
    private volatile int ticksSinceSignPlace = Integer.MAX_VALUE;
    private Vector3i editorPosition;
    private int ticksOpen = -1; // -1 = editor not yet seen by the client

    public AutoSign(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.OPEN_SIGN_EDITOR) return;
        Vector3i position = new WrapperPlayServerOpenSignEditor(event).getPosition();
        SignProbe probe = player.checkManager.get(SignProbe.class);
        if (probe != null && probe.isProbeSign(position)) return;
        if (ticksSinceSignPlace > MAX_TICKS_SINCE_PLACE) return; // editing an existing sign
        player.addRealTimeTaskNext(() -> {
            editorPosition = position;
            ticksOpen = 0;
        });
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            ItemStack held = player.inventory.getItemInHand(packet.getHand());
            if (isSign(held.getType().getName().getKey())) ticksSinceSignPlace = 0;
        } else if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            if (event.isCancelled() || editorPosition == null || ticksOpen < 0) return;
            WrapperPlayClientUpdateSign packet = new WrapperPlayClientUpdateSign(event);
            if (!packet.getBlockPosition().equals(editorPosition)) return;
            int typed = textLength(packet.getTextLines());
            if (isTooFast(ticksOpen, typed)) {
                flag("wrote " + typed + " characters " + ticksOpen + " ticks after the editor opened");
            } else {
                reward();
            }
            editorPosition = null;
            ticksOpen = -1;
        } else if (isTickPacket(event.getPacketType())) {
            if (ticksSinceSignPlace != Integer.MAX_VALUE) ticksSinceSignPlace++;
            if (ticksOpen >= 0 && ++ticksOpen > 20 * 60) {
                editorPosition = null;
                ticksOpen = -1;
            }
        }
    }

    public static boolean isSign(@NotNull String itemKey) {
        return itemKey.endsWith("_sign");
    }

    public static int textLength(String[] lines) {
        int total = 0;
        if (lines == null) return 0;
        for (String line : lines) if (line != null) total += line.strip().length();
        return total;
    }

    /** Pure rule, unit-tested: any text within 3 client ticks (150 ms) of seeing the editor. */
    public static boolean isTooFast(int ticksOpen, int typedCharacters) {
        return typedCharacters > 0 && ticksOpen <= MAX_TICKS_TO_TYPE;
    }
}
