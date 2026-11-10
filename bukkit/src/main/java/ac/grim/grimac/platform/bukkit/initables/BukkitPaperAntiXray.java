package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.manager.protection.PaperAntiXrayConfig;
import ac.grim.grimac.utils.anticheat.LogUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * NoSkillAntiCheat: with anti-xray.enable-paper-anti-xray: true, turns on the built-in anti-xray of
 * Paper / Purpur / Folia in config/paper-world-defaults.yml. Paper reads that file at startup before
 * plugins load, so the change applies after the next restart. Spigot has no built-in anti-xray.
 */
public class BukkitPaperAntiXray implements StartableInitable {
    @Override
    public void start() {
        ConfigManager config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        if (!config.getBooleanElse("anti-xray.enable-paper-anti-xray", true)) return;
        int engineMode = Math.max(1, Math.min(3, config.getIntElse("anti-xray.engine-mode", 2)));
        File file = new File("config", "paper-world-defaults.yml");
        if (!file.isFile()) {
            LogUtil.warn("Anti-Xray: config/paper-world-defaults.yml not found. The built-in anti-xray needs Paper, Purpur or Folia (Spigot has none).");
            return;
        }
        try {
            String original = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String edited = PaperAntiXrayConfig.enable(original, engineMode);
            if (edited.equals(original)) return;
            Files.writeString(file.toPath(), edited, StandardCharsets.UTF_8);
            LogUtil.warn("Anti-Xray: enabled Paper's built-in anti-xray (engine-mode " + engineMode
                    + ") in config/paper-world-defaults.yml. Restart the server once to activate it.");
        } catch (Exception e) {
            LogUtil.error("Anti-Xray: could not update config/paper-world-defaults.yml", e);
        }
    }
}
