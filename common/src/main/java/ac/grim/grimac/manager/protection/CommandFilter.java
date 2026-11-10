package ac.grim.grimac.manager.protection;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * NoSkillAntiCheat: decides which commands players may run or see, so plugin lists cannot be
 * read with /pl, /version, /help, tab completion or namespaced commands ("plugin:command").
 * Pure logic; the platform listener applies it.
 */
public final class CommandFilter {
    public static final String BYPASS_PERMISSION = "nsac.commands.bypass";
    public static final List<String> DEFAULT_BLOCKED = List.of(
            "pl", "plugins", "?", "help", "version", "ver", "about", "icanhasbukkit",
            "paper", "spigot", "purpur", "folia");

    private CommandFilter() {}

    /** "/Bukkit:PL foo" -> "bukkit:pl" */
    public static @NotNull String label(@NotNull String message) {
        String trimmed = message.strip();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        int space = trimmed.indexOf(' ');
        return (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT);
    }

    /** May a player without the bypass permission run this command label? */
    public static boolean isBlocked(@NotNull String label, @NotNull Set<String> blocked, boolean blockNamespaced) {
        String lower = label.toLowerCase(Locale.ROOT);
        int colon = lower.lastIndexOf(':');
        if (colon >= 0 && blockNamespaced) return true;
        String base = colon >= 0 ? lower.substring(colon + 1) : lower;
        return blocked.contains(lower) || blocked.contains(base);
    }

    /** Should this entry be removed from the command list the client receives (tab completion)? */
    public static boolean hideFromList(@NotNull String command, @NotNull Set<String> blocked, boolean hideNamespaced) {
        return isBlocked(command, blocked, hideNamespaced);
    }
}
