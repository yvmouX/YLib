package cn.yvmou.ylib.utils;

import cn.yvmou.ylib.YlibR;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 配置相关工具
 */
public class ConfigUtils {
    /**
     * 私有构造函数，防止实例化。
     */
    private ConfigUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * 根据版本号更新配置文件
     */
    public static void checkConfigVersion() {
        String pluginVersion = YlibR.getInstance().getDescription().getVersion();
        String configVersion = YlibR.getInstance().getConfig().getString("version", "none");

        if (!configVersion.equals(pluginVersion)) {
            // 备份旧配置
            File configFile = new File(YlibR.getInstance().getDataFolder(), "config.yml");
            File backupFile = new File(YlibR.getInstance().getDataFolder(), "config_old_v" + configVersion + ".yml");
            if (configFile.exists()) {
                configFile.renameTo(backupFile);
            }

            // 重新加载默认配置
            YlibR.getInstance().saveDefaultConfig();

            // 合并用户自定义的配置
            YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(backupFile);

            for (String key : oldConfig.getKeys(true)) {
                // 优先保留用户自定义（旧配置）内容
                newConfig.set(key, oldConfig.get(key));
            }

            // 更新版本号
            newConfig.set("version", pluginVersion);
            try {
                newConfig.save(configFile);
            } catch (Exception e) {
                LoggerUtils.error("保存新配置文件时出错: " + e.getMessage());
            }

            // 5. 重新加载配置到内存
            YlibR.getInstance().reloadConfig();

            LoggerUtils.info(ChatColor.GREEN + "配置文件已更新至 v" + pluginVersion + "，旧配置已备份。");
        }
    }
}
