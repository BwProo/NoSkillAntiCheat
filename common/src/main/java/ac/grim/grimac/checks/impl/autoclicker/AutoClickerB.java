package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

// NoSkillAntiCheat (idea from public Vulcan check descriptions): click delays too consistent.
// Humans clicking 9+ CPS vary their timing; a very low standard deviation points to a macro.
@CheckData(name = "AutoClickerB", stableKey = "nsac.autoclicker.deviation", description = "Click timing was too consistent", experimental = true, decay = 0.25)
public class AutoClickerB extends AbstractClickCheck {
    private double maxStdDev;

    public AutoClickerB(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void onClickConfig(@NotNull ConfigManager config) {
        maxStdDev = config.getDoubleElse(getConfigName() + ".max-std-dev", 0.45);
    }

    @Override
    protected void evaluate(@NotNull ClickStats stats) {
        if (stats.stdDevTicks() < maxStdDev) {
            flag(describe(stats));
        } else {
            reward();
        }
    }
}
