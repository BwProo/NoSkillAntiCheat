package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * NoSkillAntiCheat (idea from Vulcan's "Improbable" category): flags only when several
 * <em>different</em> checks of one category flag within a time window, so one over-sensitive
 * check alone never triggers it. Counts real flags and shadow flags of experimental checks.
 */
@CheckData(name = "Improbable", stableKey = "nsac.improbable", description = "Several different checks of one category flagged in a short time", experimental = true, decay = 0.1)
public class Improbable extends Check {
    public enum Category { COMBAT, MOVEMENT, BUILD }

    private static final Map<String, Category> CATEGORIES = new HashMap<>();

    static {
        for (String name : Set.of("Reach", "Hitboxes", "AutoClickerA", "AutoClickerB", "AutoClickerC",
                "AimGCD", "AimSmooth", "AimModulo360", "AimDuplicateLook", "KillAuraSwitch",
                "MultiInteractA", "MultiInteractB", "PacketOrderB", "MultiActionsA", "InvalidInteractCursor",
                "CrystalTiming", "InvalidInteractTarget", "TriggerBot", "AnchorTiming", "AICombat", "KillAuraAim")) {
            CATEGORIES.put(name, Category.COMBAT);
        }
        for (String name : Set.of("Simulation", "GroundSpoof", "NoFall", "Timer", "TimerLimit", "NoSlow",
                "Phase", "AntiKB", "AntiExplosion", "ElytraA", "ElytraB", "ElytraC", "InventoryMove")) {
            CATEGORIES.put(name, Category.MOVEMENT);
        }
        for (String name : Set.of("RotationPlace", "FarPlace", "PositionPlace", "AirLiquidPlace", "FabricatedPlace",
                "DuplicateRotPlace", "MultiPlace", "FastBreak", "MultiBreak", "FarBreak", "RotationBreak")) {
            CATEGORIES.put(name, Category.BUILD);
        }
    }

    // No field initializer: Check's constructor calls onReload() before subclass initializers run.
    private Map<Category, SignalWindow> windows;
    private long cooldownMillis;
    private long lastFlag;

    public Improbable(GrimPlayer player) {
        super(player);
    }

    public static Category categoryOf(@NotNull String checkName) {
        return CATEGORIES.get(checkName);
    }

    /** Hook from {@link Check}: forwards every real or shadow flag of {@code source}. */
    public static void signal(@NotNull GrimPlayer player, @NotNull Check source) {
        if (source instanceof Improbable || player.checkManager == null) return;
        Improbable improbable = player.checkManager.get(Improbable.class);
        if (improbable != null) improbable.onSignal(source.getCheckName(), System.currentTimeMillis());
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        long window = (long) (config.getDoubleElse(getConfigName() + ".window-seconds", 60.0) * 1000);
        int minDistinct = config.getIntElse(getConfigName() + ".min-distinct-checks", 3);
        cooldownMillis = (long) (config.getDoubleElse(getConfigName() + ".cooldown-seconds", 30.0) * 1000);
        if (windows == null) windows = new HashMap<>();
        for (Category category : Category.values()) {
            windows.computeIfAbsent(category, c -> new SignalWindow(window, minDistinct)).configure(window, minDistinct);
        }
    }

    void onSignal(@NotNull String checkName, long now) {
        Category category = CATEGORIES.get(checkName);
        if (category == null) return;
        SignalWindow window = windows == null ? null : windows.get(category);
        if (window == null) return;
        if (window.add(checkName, now) && now - lastFlag >= cooldownMillis) {
            lastFlag = now;
            String checks = String.join(", ", window.active());
            window.clear();
            flag(category.name().toLowerCase() + ": " + checks);
        }
    }
}
