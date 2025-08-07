package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.common.services.LoggerService;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 命令配置管理器
 * <p>
 * 负责管理命令的配置文件，包括自动生成配置、读取配置等功能。
 * 支持命令别名、权限、使用限制等配置。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class CommandConfig {
    private final Plugin plugin;
    private final LoggerService logger;

    private File configFile;
    private FileConfiguration config;

    public CommandConfig(Plugin plugin, LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.configFile = new File(plugin.getDataFolder(), "commands.yml");
        loadConfig();
    }

    public void initCommandConfig(String commandName, SubCommand... subCommands) {
        boolean configCreated = false;
        boolean configChanged = false;

        if (!configFile.exists()) {
            configCreated = true;
            logger.warn("Config file doesn't exist. Creating new one.");
        }

        for (SubCommand subCommand : subCommands) {
            try {
                Method executeMethod = subCommand.getClass().getDeclaredMethod("execute",
                        CommandSender.class, String[].class);

                if (executeMethod.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
                    String basePath = "commands." + commandName + "." + options.name();

                    // 检查配置是否存在，不存在创建默认配置
                    if (!config.contains(basePath)) {
                        config.set(basePath + ".permission", options.permission().isEmpty() ? "none" : options.permission());
                        config.set(basePath + ".onlyPlayer", options.onlyPlayer());
                        config.set(basePath + ".register", options.register());
                        config.set(basePath + ".usage", options.usage().isEmpty() ? "none" : options.usage());
                        config.set(basePath + ".alias", options.alias().length < 1 ? "none" : options.alias());

                        configChanged = true;

                        logger.warn("The command configuration file does not exit, a default configuration file will be created.");
                    }
                }
            } catch (NoSuchMethodException e) {
                logger.error("subCommand " + subCommand.getClass().getSimpleName() + " no correct method was found");
            }
        }

        if (configChanged || configCreated) {
            try {
                config.save(configFile);
                logger.warn("The command configuration file has been saved.");
            } catch (IOException e) {
                logger.error("Could not save command config file " + configFile.getAbsolutePath() + e.getMessage());
            }
        }
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 创建数据目录
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                logger.info("The command configuration file has been created: " + configFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Could not create config file " + configFile.getAbsolutePath());
            }
        }

        // 总是加载配置文件，无论文件是否存在
        config = YamlConfiguration.loadConfiguration(configFile);

        // 如果配置文件是新创建的，添加头部注释
        if (!config.contains("_info")) {
            config.set("_info", Arrays.asList(
                    "YLib 命令配置文件",
                    "该文件用于配置插件命令的各种选项",
                    "register: 是否注册该命令",
                    "permission: 执行命令所需权限",
                    "onlyPlayer: 是否只允许玩家执行",
                    "alias: 命令别名列表",
                    "usage: 命令使用方法"
            ));
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.error("Could not save config file " + configFile.getAbsolutePath());
        }

    }

    /**
     * 获取配置文件
     * @return 配置文件实例
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * 获取子命令列表
     * @param mainCommand 主命令
     * @return 子命令列表
     */
    public List<String> getSubCommandList(String mainCommand) {
        ConfigurationSection configSection = config.getConfigurationSection("commands." + mainCommand);
        if (configSection == null) { return null; }

        return new ArrayList<>(configSection.getKeys(false));
    }
    /**
     * 获取命令别名列表
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 别名列表
     */
    @NotNull
    public List<String> getCommandAliases(@NotNull String commandName, @NotNull String subCommandName) {
        String path = "commands." + commandName + "." + subCommandName + ".alias";
        return config.getStringList(path);
    }

    /**
     * 获取命令使用方法
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 使用方法字符串
     */
    @NotNull
    public String getCommandUsage(@NotNull String commandName, @NotNull String subCommandName) {
        String path = "commands." + commandName + "." + subCommandName + ".usage";
        return config.getString(path, "/" + commandName + " " + subCommandName);
    }

    /**
     * 检查命令是否只允许玩家执行
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 如果只允许玩家执行返回true，否则返回false
     */
    public boolean isPlayerOnly(@NotNull String commandName, @NotNull String subCommandName) {
        String path = "commands." + commandName + "." + subCommandName + ".onlyPlayer";
        return config.getBoolean(path, false);
    }

    /**
     * 获取命令权限
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 权限字符串，可能为空
     */
    @NotNull
    public String getCommandPermission(@NotNull String commandName, @NotNull String subCommandName) {
        String path = "commands." + commandName + "." + subCommandName + ".permission";
        return config.getString(path, "");
    }

    /**
     * 检查命令是否启用
     *
     * @param commandName 主命令名称
     * @param subCommandName 子命令名称
     * @return 如果启用返回true，否则返回false
     */
    public boolean isCommandEnabled(@NotNull String commandName, @NotNull String subCommandName) {
        String path = "commands." + commandName + "." + subCommandName + ".register";
        return config.getBoolean(path, true);
    }
}
