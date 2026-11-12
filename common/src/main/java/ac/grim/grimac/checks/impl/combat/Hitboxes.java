package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "Hitboxes", stableKey = "grim.combat.hitboxes", description = "Tried to hit an entity outside its valid hitbox")
public class Hitboxes extends Check {
    public Hitboxes(GrimPlayer player) {
        super(player);
    }

    /** NoSkillAntiCheat: flag, labelled "CrystalAura (hit without looking)" when the entity is an end crystal. */
    public void flagHit(String verbose, String entityKey) {
        String aura = ac.grim.grimac.utils.anticheat.AuraItems.forEntity(entityKey);
        if (aura != null) flagAs(aura + " (hit without looking)");
        else clearFlagAs();
        flag(verbose);
    }
}
