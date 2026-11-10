package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.Constants;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * NoSkillAntiCheat (idea from Vulcan's /vulcan knockback): sends a player a hit-like knockback so
 * staff can see whether AntiKB flags (Velocity/AntiKnockback cheats ignore it).
 */
public class GrimKnockback implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager, CloudPlatformCommandArguments arguments) {
        commandManager.command(commandManager.commandBuilder(Constants.COMMAND_ROOT, Constants.COMMAND_ALIASES)
                .literal("knockback", "kb")
                .permission("nsac.knockback")
                .required("target", arguments.singlePlayerSelectorParser())
                .handler(this::handleKnockback));
    }

    private void handleKnockback(@NotNull CommandContext<Sender> context) {
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
        // Roughly a vanilla melee hit: 0.4 horizontal in a random direction, 0.36 up
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        Vector3d velocity = new Vector3d(Math.cos(angle) * 0.4, 0.36, Math.sin(angle) * 0.4);
        // sendPacket (not silent) so Grim's KnockbackHandler tracks it like real knockback
        grimPlayer.user.sendPacket(new WrapperPlayServerEntityVelocity(grimPlayer.entityID, velocity));
        sender.sendMessage(Component.text("Sent test knockback to " + grimPlayer.getName()
                + ". If they did not move, AntiKB should flag.", NamedTextColor.GREEN));
    }
}
