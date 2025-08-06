package cn.yvmou.ylib.core;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.common.services.ConfigurationService;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.services.MessageService;
import cn.yvmou.ylib.common.utils.VersionUtils;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YLib核心类 - 提供Minecraft插件开发的核心功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class YLibCore {
    
    public static YLibCore instance;
    private final JavaPlugin plugin;
    private final LoggerService loggerService;
    private final ConfigurationService configService;
    private final MessageService messageService;
    private final UniversalScheduler universalScheduler;
    private final CommandManager commandManager;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public YLibCore(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
        instance = this;
        
        // 初始化服务
        this.loggerService = new LoggerService(plugin);
        this.configService = new ConfigurationService(plugin, loggerService);
        this.messageService = new MessageService(plugin, loggerService);
        
        // 初始化调度器和命令管理器（这些将在平台特定模块中实现）
        this.universalScheduler = createSchedulerManager();
        this.commandManager = createCommandManager();
        
        // 记录初始化信息
        loggerService.info("YLib Core initialized successfully");
        loggerService.info("Server type: " + VersionUtils.getServerType());
        loggerService.info("Server version: " + VersionUtils.getServerVersion());
    }
    
    /**
     * 获取实例
     * @return YLibCore 实例
     */
    public static YLibCore getInstance() {
        return instance;
    }
    
    /**
     * 获取插件实例
     * @return JavaPlugin 插件实例
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    /**
     * 获取日志服务
     * @return LoggerService 日志服务实例
     */
    public LoggerService getLogger() {
        return loggerService;
    }
    
    /**
     * 获取配置服务
     * @return ConfigurationService 配置服务实例
     */
    public ConfigurationService getConfig() {
        return configService;
    }
    
    /**
     * 获取消息服务
     * @return MessageService 消息服务实例
     */
    public MessageService getMessages() {
        return messageService;
    }
    
    /**
     * 获取调度器管理器
     * @return UniversalScheduler 调度器管理器实例
     */
    public UniversalScheduler getScheduler() {
        return universalScheduler;
    }
    
    /**
     * 获取命令管理器
     * @return CommandManager 命令管理器实例
     */
    public CommandManager getCommands() {
        return commandManager;
    }
    
    /**
     * 创建调度器管理器
     * @return UniversalScheduler 调度器管理器实例
     */
    private UniversalScheduler createSchedulerManager() {
        // 这个方法将在平台特定模块中被重写
        throw new UnsupportedOperationException("UniversalScheduler must be implemented by platform-specific module");
    }
    
    /**
     * 创建命令管理器
     * @return CommandManager 命令管理器实例
     */
    private CommandManager createCommandManager() {
        // 这个方法将在平台特定模块中被重写
        throw new UnsupportedOperationException("CommandManager must be implemented by platform-specific module");
    }
    
    /**
     * 检查是否为Folia服务器
     * @return boolean 如果是Folia服务器返回true
     */
    public boolean isFolia() {
        return VersionUtils.isFolia();
    }
    
    /**
     * 获取插件名称
     * @return String 插件名称
     */
    public String getPluginName() {
        return plugin.getName();
    }
    
    /**
     * 获取插件版本
     * @return String 插件版本
     */
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    /**
     * 获取插件前缀
     * @return String 插件前缀
     */
    public String getPrefix() {
        return "[" + getPluginName() + "] ";
    }
} 