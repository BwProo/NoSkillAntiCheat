package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.recorder.RecordingFormat;
import ac.grim.grimac.manager.recorder.SessionRecorder;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.Constants;
import ac.grim.grimac.utils.anticheat.LogUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * NoSkillAntiCheat: {@code /nsac record start <player> <label>}, {@code /nsac record stop <player>}
 * and {@code /nsac record status}. Labels such as "legit-pvp" or "cheat-killaura" become the training
 * labels for the AI. Only record players who agreed to it.
 */
public class GrimRecord implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command.Builder<Sender> record = commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                .literal("record").permission("grim.record");
        commandManager.command(record.literal("start")
                .required("target", arguments.singlePlayerSelectorParser())
                .required("label", StringParser.stringParser())
                .handler(this::handleStart));
        commandManager.command(record.literal("stop")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleStop));
        commandManager.command(record.literal("status").handler(this::handleStatus));
    }

    private @Nullable SessionRecorder recorderOf(Sender sender, PlayerSelector selector) {
        PlatformPlayer platformPlayer = selector.getSinglePlayer().getPlatformPlayer();
        if (platformPlayer == null || platformPlayer.isExternalPlayer()) {
            sender.sendMessage(Component.text("This player isn't on this server.", NamedTextColor.RED));
            return null;
        }
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(platformPlayer.getUniqueId());
        if (grimPlayer == null || grimPlayer.checkManager == null) {
            sender.sendMessage(Component.text("Player is exempt or offline.", NamedTextColor.RED));
            return null;
        }
        return grimPlayer.checkManager.get(SessionRecorder.class);
    }

    private void handleStart(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String label = context.get("label");
        if (!RecordingFormat.LABEL.matcher(label).matches()) {
            sender.sendMessage(Component.text("Label must be 1-32 characters: letters, digits, _ or -", NamedTextColor.RED));
            return;
        }
        SessionRecorder recorder = recorderOf(sender, context.get("target"));
        if (recorder == null) return;
        int maxMinutes = GrimAPI.INSTANCE.getConfigManager().getConfig().getIntElse("recorder.max-minutes", 30);
        try {
            File file = recorder.start(label, maxMinutes);
            sender.sendMessage(Component.text("Recording '" + label + "' started (max " + maxMinutes + " min): "
                    + file.getName(), NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Could not start the recording, see console.", NamedTextColor.RED));
            LogUtil.error("Failed to start recording", e);
        }
    }

    private void handleStop(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        SessionRecorder recorder = recorderOf(sender, context.get("target"));
        if (recorder == null) return;
        File file = recorder.stop();
        sender.sendMessage(file == null
                ? Component.text("No recording is running for this player.", NamedTextColor.RED)
                : Component.text("Recording saved: " + file.getPath(), NamedTextColor.GREEN));
    }

    private void handleStatus(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        int running = 0;
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player.checkManager == null) continue;
            SessionRecorder recorder = player.checkManager.get(SessionRecorder.class);
            if (recorder != null && recorder.isRecording()) {
                running++;
                sender.sendMessage(Component.text(player.getName() + ": " + recorder.label(), NamedTextColor.WHITE));
            }
        }
        if (running == 0) sender.sendMessage(Component.text("No recordings are running.", NamedTextColor.GRAY));
    }
}
