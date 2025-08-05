package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.services.LoggerService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param logger 日志服务
     */
    public CommandConfig(@NotNull Plugin plugin, @NotNull LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.configFile = new File(plugin.getDataFolder(), "commands.yml");
        loadConfig();
    }
    
    /**
     * 初始化命令配置
     * 
     * @param commandName 主命令名称
     * @param subCommands 子命令数组
     */
    public void initCommandConfig(@NotNull String commandName, @NotNull SubCommand... subCommands) {
        boolean configChanged = false;
        
        for (SubCommand subCommand : subCommands) {
            try {
                Method executeMethod = subCommand.getClass().getDeclaredMethod("execute", 
                    org.bukkit.command.CommandSender.class, String[].class);
                
                if (executeMethod.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
                    String basePath = "commands." + commandName + "." + options.name();
                    
                    // 检查配置是否存在，不存在则创建默认配置
                    if (!config.contains(basePath)) {
                        config.set(basePath + ".permission", options.permission());
                        config.set(basePath + ".onlyPlayer", options.onlyPlayer());
                        config.set(basePath + ".register", options.register());
                        config.set(basePath + ".usage", options.usage().isEmpty() ? 
                            "/" + commandName + " " + options.name() : options.usage());
                        
                        // 设置别名
                        if (options.alias().length > 0) {
                            config.set(basePath + ".alias", Arrays.asList(options.alias()));
                        } else {
                            config.set(basePath + ".alias", Arrays.asList(options.name()));
                        }
                        
                        configChanged = true;
                        logger.debug("为命令 " + options.name() + " 创建了默认配置");
                    }
                }
            } catch (NoSuchMethodException e) {
                logger.warning("子命令类 " + subCommand.getClass().getSimpleName() + " 没有正确的execute方法");
            }
        }
        
        if (configChanged) {
            saveConfig();
            logger.info("命令配置文件已更新: " + configFile.getName());
        }
    }
    
    /**
     * 获取配置文件
     * 
     * @return 配置文件实例
     */
    @NotNull
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
        logger.info("命令配置已重新加载");
    }
    
    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.error("保存命令配置文件失败: " + e.getMessage(), e);
        }
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
     * 加载配置文件
     */
    private void loadConfig() {
        if (!configFile.exists()) {
            // 创建数据目录
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            try {
                configFile.createNewFile();
                logger.info("创建了新的命令配置文件: " + configFile.getName());
            } catch (IOException e) {
                logger.error("创建命令配置文件失败: " + e.getMessage(), e);
            }
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 添加配置文件头部注释
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
    }
}