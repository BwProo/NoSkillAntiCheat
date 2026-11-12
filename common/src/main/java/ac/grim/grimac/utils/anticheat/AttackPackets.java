package ac.grim.grimac.utils.anticheat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: one place that recognises a melee attack. Clients before 26.1 send
 * INTERACT_ENTITY with action ATTACK; 26.1+ clients send the dedicated ATTACK packet.
 */
public final class AttackPackets {
    public static final int NONE = Integer.MIN_VALUE;

    private AttackPackets() {
    }

    /** Entity id of the attacked entity, or {@link #NONE} when the packet is not an attack. */
    public static int attackedEntity(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            return new WrapperPlayClientAttack(event).getEntityId();
        }
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) return packet.getEntityId();
        }
        return NONE;
    }

    /**
     * Like {@link #attackedEntity} but only for attacks on players: KillAura-style checks must not
     * count end crystals, mobs or other entities (crystal PvP has its own checks).
     */
    public static int attackedPlayer(@NotNull ac.grim.grimac.player.GrimPlayer player, @NotNull PacketReceiveEvent event) {
        int id = attackedEntity(event);
        if (id == NONE) return NONE;
        var entity = player.compensatedEntities.getEntity(id);
        return entity != null && entity.getType() == com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER ? id : NONE;
    }

    /** True for an INTERACT_ENTITY that is not an attack (right-click on an entity). */
    public static boolean isNonAttackInteract(@NotNull PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                && new WrapperPlayClientInteractEntity(event).getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK;
    }
}
