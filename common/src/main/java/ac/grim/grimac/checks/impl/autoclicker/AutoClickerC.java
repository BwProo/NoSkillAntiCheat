package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import org.jetbrains.annotations.NotNull;

// NoSkillAntiCheat (idea from public Vulcan check descriptions): too few distinct click delays.
@CheckData(name = "AutoClickerC", stableKey = "nsac.autoclicker.distinct", description = "Used too few distinct click delays", experimental = true, decay = 0.25)
public class AutoClickerC extends AbstractClickCheck {
    private int minDistinct;

    public AutoClickerC(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void onClickConfig(@NotNull ConfigManager config) {
        minDistinct = config.getIntElse(getConfigName() + ".min-distinct", 3);
    }

    @Override
    protected void evaluate(@NotNull ClickStats stats) {
        if (stats.distinctDelays() < minDistinct) {
            flag(describe(stats));
        } else {
            reward();
        }
    }
}
