package ac.grim.grimac.checks;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * NoSkillAntiCheat: readable default names for alerts, so staff see at once which cheat was
 * detected (English). The internal check name stays the ID (permissions, config, punishments); a configured
 * "<Check>.displayname" still wins. Families (BadPacketsA, ElytraC ...) keep their letter.
 */
public final class CheckNames {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final String[][] FAMILIES = {
            {"BadPackets", "BadPackets"}, {"PacketOrder", "Packet order"}, {"Crash", "Crash attempt"},
            {"Exploit", "Exploit"}, {"Chat", "Chat exploit"}, {"Elytra", "ElytraFly"}, {"Vehicle", "BoatFly/EntitySpeed"},
            {"Sprint", "Sprint"}, {"MultiActions", "Multi action"}, {"PositionBreak", "Nuker (position)"},
    };

    static {
        put("Simulation", "Movement");
        put("GroundSpoof", "GroundSpoof");
        put("NoFall", "NoFall");
        put("Phase", "Phase (through blocks)");
        put("Timer", "Timer (game speed)");
        put("TimerLimit", "Timer (ping abuse)");
        put("TickTimer", "Timer (ticks)");
        put("NegativeTimer", "Blink (timer)");
        put("VehicleTimer", "Timer (vehicle)");
        put("NoSlow", "NoSlow");
        put("AntiKB", "Velocity (anti-knockback)");
        put("AntiExplosion", "Velocity (explosion)");
        put("Reach", "Reach");
        put("Hitboxes", "Hitboxes (KillAura)");
        put("SelfInteract", "Self hit");
        put("InvalidInteractCursor", "KillAura (invalid hit point)");
        put("InvalidInteractTarget", "CrystalAura (ID prediction)");
        put("MultiInteractA", "KillAura (multiple targets)");
        put("MultiInteractB", "KillAura (multiple hit points)");
        put("AimModulo360", "Aimbot (rotation snap)");
        put("AimDuplicateLook", "Aimbot (duplicate rotation)");
        put("AimGCD", "AimAssist (sensitivity)");
        put("AimSmooth", "Aimbot (smooth rotation)");
        put("KillAuraSwitch", "KillAura (target switch)");
        put("KillAuraAim", "KillAura (aim lock)");
        put("TriggerBot", "TriggerBot");
        put("AutoClickerA", "AutoClicker (CPS)");
        put("AutoClickerB", "AutoClicker (too consistent)");
        put("AutoClickerC", "AutoClicker (pattern)");
        put("CrystalTiming", "CrystalAura/FastCrystal");
        put("AnchorTiming", "AnchorAura");
        put("AICombat", "AI: combat cheat");
        put("Improbable", "Multiple cheats (Improbable)");
        put("BlacklistedClient", "Hacked client");
        put("InventoryMove", "InvMove");
        put("QuickSwap", "QuickSwap (ShieldBreaker/MaceSwap/AutoTool)");
        put("AutoTotem", "AutoTotem");
        put("InventorySpeed", "ChestStealer/inventory macro");
        put("MultiActionsA", "AutoBlock (hit while using item)");
        put("MultiActionsB", "FastBreak (break while using item)");
        put("MultiActionsC", "InvMove (click while moving)");
        put("MultiActionsD", "InvMove (close while moving)");
        put("MultiActionsE", "Swing while using item");
        put("MultiActionsF", "Block and entity at once");
        put("MultiActionsG", "Action while rowing");
        put("FastBreak", "FastBreak");
        put("FarBreak", "Nuker (too far)");
        put("MultiBreak", "Nuker (multiple blocks)");
        put("RotationBreak", "Nuker (not looking)");
        put("NoSwingBreak", "Nuker (no swing)");
        put("AirLiquidBreak", "Break air/liquid");
        put("WrongBreak", "Break wrong block");
        put("InvalidBreak", "Invalid break");
        put("RotationPlace", "Scaffold (Rotation)");
        put("FarPlace", "Scaffold (too far)");
        put("PositionPlace", "Scaffold (Position)");
        put("AirLiquidPlace", "AirPlace");
        put("FabricatedPlace", "Scaffold (fabricated)");
        put("DuplicateRotPlace", "Scaffold (duplicate rotation)");
        put("MultiPlace", "FastPlace/Tower");
        put("InvalidPlaceFace", "Scaffold (invalid face)");
        put("InvalidPlaceCursor", "Scaffold (invalid point)");
        put("Post", "Packet order (post)");
        put("TransactionOrder", "Blink/ping spoof");
        put("FlightA", "Flight");
    }

    private CheckNames() {}

    private static void put(String id, String name) {
        EXACT.put(id, name);
    }

    /** Readable alert name for a check ID; unknown checks keep their ID. */
    public static @NotNull String friendly(@NotNull String checkName) {
        String exact = EXACT.get(checkName);
        if (exact != null) return exact;
        for (String[] family : FAMILIES) {
            if (checkName.startsWith(family[0]) && checkName.length() > family[0].length()) {
                return family[1] + " " + checkName.substring(family[0].length());
            }
        }
        return checkName;
    }
}
