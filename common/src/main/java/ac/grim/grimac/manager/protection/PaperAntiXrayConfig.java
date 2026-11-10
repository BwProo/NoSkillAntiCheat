package ac.grim.grimac.manager.protection;

import org.jetbrains.annotations.NotNull;

/**
 * NoSkillAntiCheat: switches on Paper's built-in anti-xray (also used by Purpur and Folia) by
 * editing the "anticheat.anti-xray" block of config/paper-world-defaults.yml as text, so all
 * comments and other settings stay untouched. Pure and unit-tested.
 */
public final class PaperAntiXrayConfig {
    private PaperAntiXrayConfig() {}

    /** Returns the edited file content, or the same string if nothing needed to change. */
    public static @NotNull String enable(@NotNull String yaml, int engineMode) {
        String[] lines = yaml.split("\n", -1);
        boolean inAntiXray = false;
        int blockIndent = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            int indent = line.length() - line.stripLeading().length();
            if (trimmed.equals("anti-xray:")) {
                inAntiXray = true;
                blockIndent = indent;
                continue;
            }
            if (!inAntiXray || trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (indent <= blockIndent) {
                inAntiXray = false; // left the anti-xray block
                continue;
            }
            if (indent == blockIndent + 2) {
                String pad = line.substring(0, indent);
                if (trimmed.startsWith("enabled:")) lines[i] = pad + "enabled: true";
                else if (trimmed.startsWith("engine-mode:")) lines[i] = pad + "engine-mode: " + engineMode;
            }
        }
        return String.join("\n", lines);
    }
}
