package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** NoSkillAntiCheat (idea from Vulcan's /vulcan top): online players with the most violations. */
public class GrimTop implements BuildableCommand {
    private static final int TOP = 10;

    private record Entry(String name, double total, String worstCheck, double worstVl) {}

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                .literal("top")
                .permission("nsac.top")
                .handler(this::handleTop));
    }

    private void handleTop(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        List<Entry> entries = new ArrayList<>();
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.checkManager == null) continue;
            double total = 0, worstVl = 0;
            String worst = "-";
            for (AbstractProcessor processor : player.checkManager.processors.values()) {
                if (!(processor instanceof Check check)) continue;
                double vl = check.violations;
                total += vl;
                if (vl > worstVl) {
                    worstVl = vl;
                    worst = check.getCheckName();
                }
            }
            if (total > 0) entries.add(new Entry(player.getName(), total, worst, worstVl));
        }
        entries.sort((a, b) -> Double.compare(b.total(), a.total()));
        sender.sendMessage(Component.text(Constants.PLUGIN_NAME + " top violations (online players):", NamedTextColor.AQUA));
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("No online player has violations.", NamedTextColor.GREEN));
            return;
        }
        for (int i = 0; i < Math.min(TOP, entries.size()); i++) {
            Entry e = entries.get(i);
            sender.sendMessage(Component.text(String.format(Locale.ROOT, "%d. %s  total %.1f  (most: %s %.1f)",
                    i + 1, e.name(), e.total(), e.worstCheck(), e.worstVl()), NamedTextColor.WHITE));
        }
    }
}
