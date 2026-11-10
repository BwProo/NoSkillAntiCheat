package ac.grim.grimac.checks;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * NoSkillAntiCheat: readable default names for alerts, so staff see at once which cheat was
 * detected. The internal check name stays the ID (permissions, config, punishments); a configured
 * "<Check>.displayname" still wins. Families (BadPacketsA, ElytraC ...) keep their letter.
 */
public final class CheckNames {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final String[][] FAMILIES = {
            {"BadPackets", "BadPackets"}, {"PacketOrder", "Paket-Reihenfolge"}, {"Crash", "Crash-Versuch"},
            {"Exploit", "Exploit"}, {"Chat", "Chat-Exploit"}, {"Elytra", "ElytraFly"}, {"Vehicle", "BoatFly/EntitySpeed"},
            {"Sprint", "Sprint"}, {"MultiActions", "Mehrfach-Aktion"}, {"PositionBreak", "Nuker (Position)"},
    };

    static {
        put("Simulation", "Bewegung");
        put("GroundSpoof", "GroundSpoof");
        put("NoFall", "NoFall");
        put("Phase", "Phase (durch Blöcke)");
        put("Timer", "Timer (Spieltempo)");
        put("TimerLimit", "Timer (Ping-Missbrauch)");
        put("TickTimer", "Timer (Ticks)");
        put("NegativeTimer", "Blink (Timer)");
        put("VehicleTimer", "Timer (Reittier)");
        put("NoSlow", "NoSlow");
        put("AntiKB", "Velocity (AntiKnockback)");
        put("AntiExplosion", "Velocity (Explosion)");
        put("Reach", "Reach (Reichweite)");
        put("Hitboxes", "Hitboxes (KillAura)");
        put("SelfInteract", "Selbst geschlagen");
        put("InvalidInteractCursor", "KillAura (ungültiger Trefferpunkt)");
        put("InvalidInteractTarget", "CrystalAura (ID-Vorhersage)");
        put("MultiInteractA", "KillAura (mehrere Ziele)");
        put("MultiInteractB", "KillAura (mehrere Trefferpunkte)");
        put("AimModulo360", "Aimbot (Drehsprung)");
        put("AimDuplicateLook", "Aimbot (doppelte Rotation)");
        put("AimGCD", "AimAssist (Sensitivität)");
        put("AimSmooth", "Aimbot (glatte Drehung)");
        put("KillAuraSwitch", "KillAura (Zielwechsel)");
        put("KillAuraAim", "KillAura (Aim-Lock)");
        put("TriggerBot", "TriggerBot");
        put("AutoClickerA", "AutoClicker (CPS)");
        put("AutoClickerB", "AutoClicker (zu gleichmäßig)");
        put("AutoClickerC", "AutoClicker (Muster)");
        put("CrystalTiming", "CrystalAura/FastCrystal");
        put("AnchorTiming", "AnchorAura");
        put("AICombat", "KI: Kampf-Cheat");
        put("Improbable", "Mehrere Cheats (Improbable)");
        put("BlacklistedClient", "Cheat-Client");
        put("InventoryMove", "InvMove");
        put("InventorySpeed", "ChestStealer/Inventar-Makro");
        put("MultiActionsA", "AutoBlock (Schlag beim Benutzen)");
        put("MultiActionsB", "FastBreak (Abbau beim Benutzen)");
        put("MultiActionsC", "InvMove (Klick beim Laufen)");
        put("MultiActionsD", "InvMove (Schließen beim Laufen)");
        put("MultiActionsE", "Schwung beim Benutzen");
        put("MultiActionsF", "Block und Entity gleichzeitig");
        put("MultiActionsG", "Aktion beim Rudern");
        put("FastBreak", "FastBreak");
        put("FarBreak", "Nuker (zu weit)");
        put("MultiBreak", "Nuker (mehrere Blöcke)");
        put("RotationBreak", "Nuker (ohne Hinsehen)");
        put("NoSwingBreak", "Nuker (ohne Armschwung)");
        put("AirLiquidBreak", "Abbau in Luft/Flüssigkeit");
        put("WrongBreak", "Abbau des falschen Blocks");
        put("InvalidBreak", "Ungültiger Abbau");
        put("RotationPlace", "Scaffold (Rotation)");
        put("FarPlace", "Scaffold (zu weit)");
        put("PositionPlace", "Scaffold (Position)");
        put("AirLiquidPlace", "AirPlace");
        put("FabricatedPlace", "Scaffold (gefälscht)");
        put("DuplicateRotPlace", "Scaffold (doppelte Rotation)");
        put("MultiPlace", "FastPlace/Tower");
        put("InvalidPlaceFace", "Scaffold (ungültige Seite)");
        put("InvalidPlaceCursor", "Scaffold (ungültiger Punkt)");
        put("Post", "Paket-Reihenfolge (Post)");
        put("TransactionOrder", "Blink/Ping-Spoof");
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
