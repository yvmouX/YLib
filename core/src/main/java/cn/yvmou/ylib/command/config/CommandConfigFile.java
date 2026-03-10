package cn.yvmou.ylib.command.config;

import cn.yvmou.ylib.api.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * 负责管理 commands.yml 文件的物理操作（创建、加载、保存）
 */
public class CommandConfigFile {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;
    private YamlConfiguration config;
    
    private static final String FILE_NAME = "commands.yml";

    public CommandConfigFile(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    /**
     * 加载 commands.yml 文件
     * 如果文件不存在，则尝试从 JAR 复制或创建空文件
     */
    public void load() {
        if (!file.exists()) {
            if (plugin.getResource(FILE_NAME) != null) {
                plugin.saveResource(FILE_NAME, false);
            } else {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException e) {
                    logger.error("Failed to create commands.yml", e);
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * 获取指定命令的配置段
     * @param commandName 命令名称 (e.g. "example")
     * @return 对应的配置段，如果不存在则返回 null
     */
    public ConfigurationSection getCommandSection(String commandName) {
        // 直接从根节点获取配置
        return config.getConfigurationSection(commandName);
    }
    
    /**
     * 检查是否存在指定命令的配置
     */
    public boolean hasCommandConfig(String commandName) {
        return getCommandSection(commandName) != null;
    }
    
    /**
     * 获取或创建指定命令的配置段（用于写入默认值）
     */
    public ConfigurationSection createCommandSection(String commandName) {
        // 直接在根节点下创建
        return config.createSection(commandName);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
    
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.error("Failed to save commands.yml", e);
        }
    }
}
