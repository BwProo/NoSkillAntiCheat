package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

// NoSkillAntiCheat: the upstream Grim update check (HTTP request to Grim's API) was removed,
// so this command only prints the local version and never contacts an external server.
public class GrimVersion implements BuildableCommand {

    public static void sendVersion(Sender sender) {
        sender.sendMessage(Component.text()
                .append(Component.text(Constants.PLUGIN_NAME + " Version: ").color(NamedTextColor.GRAY))
                .append(Component.text(GrimAPI.INSTANCE.getExternalAPI().getGrimVersion()).color(NamedTextColor.LIGHT_PURPLE))
                .build());
        // Attribution required by the GPL-3.0; this is the one in-game place that names the origin.
        sender.sendMessage(Component.text("Based on GrimAC - licensed under GPL-3.0", NamedTextColor.DARK_GRAY));
    }

    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(
                commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                        .literal("version")
                        .permission("nsac.version")
                        .handler(this::handleVersion)
        );
    }

    private void handleVersion(@NotNull CommandContext<Sender> context) {
        sendVersion(context.sender());
    }
}
