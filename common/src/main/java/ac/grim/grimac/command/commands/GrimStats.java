package ac.grim.grimac.command.commands;

import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.stats.FlagStats;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.Constants;
import ac.grim.grimac.utils.anticheat.LogUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * NoSkillAntiCheat: {@code /nsac stats} and {@code /nsac session start <label> | stop}
 * for measuring false positives per check.
 */
public class GrimStats implements BuildableCommand {
    private static final Pattern LABEL = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final int TOP = 10;

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> root = commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES);
        commandManager.command(root.literal("stats").permission("grim.stats").handler(this::handleStats));
        Command.Builder<Sender> session = root.literal("session").permission("grim.stats");
        commandManager.command(session.literal("start")
                .required("label", StringParser.stringParser())
                .handler(this::handleStart));
        commandManager.command(session.literal("stop").handler(this::handleStop));
    }

    private void handleStats(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        FlagStats.Window session = FlagStats.INSTANCE.session();
        send(sender, session != null ? session : FlagStats.INSTANCE.total());
    }

    private void handleStart(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String label = context.get("label");
        if (!LABEL.matcher(label).matches()) {
            sender.sendMessage(Component.text("Label must be 1-32 characters: letters, digits, _ or -", NamedTextColor.RED));
            return;
        }
        FlagStats.Window previous = FlagStats.INSTANCE.startSession(label);
        if (previous != null) {
            sender.sendMessage(Component.text("Previous session '" + previous.label + "' was discarded.", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Session '" + label + "' started. Play normally, then run /"
                + Constants.COMMAND_ROOT + " session stop.", NamedTextColor.GREEN));
    }

    private void handleStop(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        FlagStats.Window session = FlagStats.INSTANCE.session();
        if (session == null) {
            sender.sendMessage(Component.text("No session is running.", NamedTextColor.RED));
            return;
        }
        send(sender, session);
        try {
            File file = FlagStats.INSTANCE.stopSession();
            if (file != null) sender.sendMessage(Component.text("Report saved: " + file.getPath(), NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Could not save the report, see console.", NamedTextColor.RED));
            LogUtil.error("Failed to write session report", e);
        }
    }

    private static void send(Sender sender, FlagStats.Window window) {
        double hours = window.playerHours();
        sender.sendMessage(Component.text(String.format(Locale.ROOT, "%s stats '%s': %.2f player-hours",
                Constants.PLUGIN_NAME, window.label, hours), NamedTextColor.AQUA));
        List<Map.Entry<String, FlagStats.Counter>> checks = FlagStats.sortedChecks(window);
        if (checks.isEmpty()) {
            sender.sendMessage(Component.text("No flags so far.", NamedTextColor.GREEN));
            return;
        }
        for (Map.Entry<String, FlagStats.Counter> e : checks.subList(0, Math.min(TOP, checks.size()))) {
            FlagStats.Counter c = e.getValue();
            sender.sendMessage(Component.text()
                    .append(Component.text(e.getKey() + (c.experimental ? " (exp.)" : ""), NamedTextColor.WHITE))
                    .append(Component.text(String.format(Locale.ROOT, "  flags %d (%s/h)  setbacks %d  shadow %d",
                            c.flags.sum(), hours > 0 ? String.format(Locale.ROOT, "%.1f", c.flags.sum() / hours) : "-",
                            c.setbacks.sum(), c.shadowFlags.sum()), NamedTextColor.GRAY))
                    .build());
        }
        if (checks.size() > TOP) {
            sender.sendMessage(Component.text("... " + (checks.size() - TOP) + " more in the session report.", NamedTextColor.GRAY));
        }
    }
}
