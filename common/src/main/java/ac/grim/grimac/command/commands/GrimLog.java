package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.init.start.SuperDebug;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.Constants;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

// NoSkillAntiCheat: upstream Grim uploaded these logs to paste.grim.ac.
// They are now written to the plugin's own "logs" folder, so no player data leaves the server.
public class GrimLog implements BuildableCommand {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public static void sendLogAsync(Sender sender, String log, Consumer<String> consumer, String type) {
        String success = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log", "%prefix% &fSaved debug log to: %url%");
        String failure = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-upload-failure", "%prefix% &cSomething went wrong while saving this log, see console for more information.");
        String saving = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("upload-log-start", "%prefix% &fSaving log... please wait");
        saving = MessageUtil.replacePlaceholders(sender, saving);
        sender.sendMessage(MessageUtil.miniMessage(saving));
        GrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runNow(GrimAPI.INSTANCE.getGrimPlugin(), () -> {
            try {
                File file = writeLog(log, type);
                String message = success.replace("%url%", file.getPath());
                consumer.accept(message);
                sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, message)));
            } catch (Exception e) {
                sender.sendMessage(MessageUtil.miniMessage(MessageUtil.replacePlaceholders(sender, failure)));
                LogUtil.error("Failed to save log", e);
            }
        });
    }

    private static File writeLog(String log, String type) throws IOException {
        File folder = new File(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder(), "logs");
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Could not create " + folder);
        }
        String extension = type != null && type.contains("yaml") ? ".yml" : ".txt";
        File file = new File(folder, "log-" + LocalDateTime.now().format(FILE_TIME) + extension);
        Files.write(file.toPath(), log.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        Command<Sender> command = commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                .literal("log", "logs")
                .permission("nsac.log")
                .required("flagId", IntegerParser.integerParser())
                .handler(this::handleLog)
                .manager(commandManager)
                .build();
        commandManager
                .command(command)
                .command(commandManager.commandBuilder("gl").proxies(command));
    }

    private void handleLog(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        int flagId = context.get("flagId");

        StringBuilder builder = SuperDebug.getFlag(flagId);
        if (builder == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "upload-log-not-found", "%prefix% &cUnable to find that log"));
            return;
        }
        sendLogAsync(sender, builder.toString(), string -> {}, "text/yaml");
    }
}
