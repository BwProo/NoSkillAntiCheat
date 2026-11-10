package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.protection.CommandFilter;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * NoSkillAntiCheat: hides the server's plugins from players. Without nsac.commands.bypass a player
 * cannot run /pl, /plugins, /?, /help, /version and similar commands, cannot run namespaced
 * commands ("bukkit:pl"), and these commands are removed from the command list the client gets,
 * so tab completion does not reveal plugin names. Settings: config.yml "command-protection".
 */
@SuppressWarnings("deprecation") // ChatColor works on Paper, Spigot, Purpur and Folia alike
public class BukkitCommandProtection implements StartableInitable, Listener {

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, GrimACBukkitLoaderPlugin.LOADER);
    }

    private static ConfigManager config() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig();
    }

    private static Set<String> blocked(ConfigManager config) {
        List<String> list = config.getStringListElse("command-protection.blocked-commands", CommandFilter.DEFAULT_BLOCKED);
        Set<String> set = new HashSet<>();
        for (String s : list) set.add(s.toLowerCase(Locale.ROOT).replaceFirst("^/", ""));
        return set;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        ConfigManager config = config();
        if (!config.getBooleanElse("command-protection.enabled", true)) return;
        if (event.getPlayer().hasPermission(CommandFilter.BYPASS_PERMISSION)) return;
        String label = CommandFilter.label(event.getMessage());
        boolean blockNamespaced = config.getBooleanElse("command-protection.block-namespaced-commands", true);
        if (!CommandFilter.isBlocked(label, blocked(config), blockNamespaced)) return;
        event.setCancelled(true);
        String message = config.getStringElse("command-protection.message", "&cUnknown command.");
        if (!message.isEmpty()) event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandList(PlayerCommandSendEvent event) {
        ConfigManager config = config();
        if (!config.getBooleanElse("command-protection.enabled", true)) return;
        if (event.getPlayer().hasPermission(CommandFilter.BYPASS_PERMISSION)) return;
        Set<String> blocked = blocked(config);
        boolean hideNamespaced = config.getBooleanElse("command-protection.hide-namespaced-commands", true);
        event.getCommands().removeIf(command -> CommandFilter.hideFromList(command, blocked, hideNamespaced));
    }
}
