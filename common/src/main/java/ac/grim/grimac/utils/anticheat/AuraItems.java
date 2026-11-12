package ac.grim.grimac.utils.anticheat;

import org.jetbrains.annotations.Nullable;

/**
 * NoSkillAntiCheat: names the PvP aura behind a placement or hit, so alerts read "CrystalAura (...)"
 * instead of a generic "Scaffold" or "Hitboxes". Pure, unit-tested.
 */
public final class AuraItems {
    private AuraItems() {
    }

    /** Aura for using {@code itemKey} on a block of {@code clickedBlockKey} (either may be null). */
    public static @Nullable String forUse(@Nullable String itemKey, @Nullable String clickedBlockKey) {
        if ("end_crystal".equals(itemKey)) return "CrystalAura";
        if ("respawn_anchor".equals(itemKey) || "respawn_anchor".equals(clickedBlockKey)) return "AnchorAura";
        if (itemKey != null && itemKey.endsWith("_bed") || clickedBlockKey != null && clickedBlockKey.endsWith("_bed")) return "BedAura";
        return null;
    }

    /** Aura for attacking an entity of {@code entityKey}. */
    public static @Nullable String forEntity(@Nullable String entityKey) {
        return "end_crystal".equals(entityKey) ? "CrystalAura" : null;
    }
}
