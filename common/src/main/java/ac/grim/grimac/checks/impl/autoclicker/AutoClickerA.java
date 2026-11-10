package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

// NoSkillAntiCheat (idea from public Vulcan check descriptions): average clicks per second too high.
@CheckData(name = "AutoClickerA", stableKey = "nsac.autoclicker.cps", description = "Clicked faster than the configured maximum CPS", experimental = true, decay = 0.25)
public class AutoClickerA extends AbstractClickCheck {
    private double maxCps;

    public AutoClickerA(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void onClickConfig(@NotNull ConfigManager config) {
        maxCps = config.getDoubleElse(getConfigName() + ".max-cps", 22.0);
    }

    @Override
    protected void evaluate(@NotNull ClickStats stats) {
        if (stats.cps() > maxCps) {
            flag(describe(stats));
        } else {
            reward();
        }
    }
}
