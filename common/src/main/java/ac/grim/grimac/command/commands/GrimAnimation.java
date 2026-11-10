package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.punish.BanAnimation;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

/** NoSkillAntiCheat: {@code /nsac animation <player>} previews the ban animation without punishing. */
public class GrimAnimation implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                .literal("animation")
                .permission("nsac.animation")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handle));
    }

    private void handle(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");
        PlatformPlayer platformPlayer = target.getSinglePlayer().getPlatformPlayer();
        if (platformPlayer == null || platformPlayer.isExternalPlayer()) {
            sender.sendMessage(Component.text("This player isn't on this server.", NamedTextColor.RED));
            return;
        }
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(platformPlayer.getUniqueId());
        if (grimPlayer == null) {
            sender.sendMessage(Component.text("Player is exempt or offline.", NamedTextColor.RED));
            return;
        }
        BanAnimation.play(grimPlayer, () -> sender.sendMessage(Component.text("Animation finished (preview, nobody was punished).", NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(Component.text("Playing the ban animation for " + grimPlayer.getName() + " (preview only).", NamedTextColor.LIGHT_PURPLE));
    }
}
