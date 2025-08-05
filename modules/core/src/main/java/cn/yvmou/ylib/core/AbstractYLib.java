package cn.yvmou.ylib.core;

import cn.yvmou.ylib.api.YLibAPI;
import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.container.ServiceContainer;
import cn.yvmou.ylib.api.error.YLibErrorHandler;
import cn.yvmou.ylib.api.scheduler.SchedulerManager;
import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.common.command.SimpleCommandManager;
import cn.yvmou.ylib.common.config.SimpleConfigurationManager;
import cn.yvmou.ylib.common.container.SimpleServiceContainer;
import cn.yvmou.ylib.common.error.SimpleYLibErrorHandler;
import cn.yvmou.ylib.common.services.ConfigurationService;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.services.MessageService;
import cn.yvmou.ylib.common.utils.ValidationUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * YLib抽象基类，提供所有平台实现的通用功能
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public abstract class AbstractYLib implements YLibAPI {
    
    // 插件实例
    protected final JavaPlugin plugin;
    
    // 服务实例
    protected LoggerService loggerService;
    protected ConfigurationService configurationService;
    protected MessageService messageService;
    protected SchedulerManager schedulerManager;
    protected CommandManager commandManager;
    protected ServiceContainer serviceContainer;
    protected ConfigurationManager configurationManager;
    protected YLibErrorHandler errorHandler;
    
    // 状态管理
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    
    // 服务器类型
    protected final ServerType serverType;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param serverType 服务器类型
     * @throws YLibException 如果初始化失败
     */
    protected AbstractYLib(@NotNull JavaPlugin plugin, @NotNull ServerType serverType) throws YLibException {
        ValidationUtils.notNull(plugin, "插件实例不能为null");
        ValidationUtils.notNull(serverType, "服务器类型不能为null");
        
        this.plugin = plugin;
        this.serverType = serverType;
        
        getLogger().info("正在初始化 " + serverType.getDisplayName() + " 类型的YLib实例...");
        
        try {
            // 初始化核心服务
            initializeServices();
            
            // 初始化平台特定组件
            initializePlatformSpecific();
            
            getLogger().info("YLib实例初始化完成");
        } catch (Exception e) {
            throw new YLibException("YLib初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化核心服务
     * 
     * @throws YLibException 如果初始化失败
     */
    private void initializeServices() throws YLibException {
        try {
            // 初始化服务容器
            this.serviceContainer = new SimpleServiceContainer();
            
            // 初始化日志服务
            this.loggerService = new LoggerService(plugin);
            
            // 初始化配置服务
            this.configurationService = new ConfigurationService(plugin, this.loggerService);
            
            // 初始化消息服务
            this.messageService = new MessageService(plugin, this.loggerService);
            
            // 初始化配置管理器
            this.configurationManager = new SimpleConfigurationManager(plugin, this.loggerService);
            
                    // 初始化错误处理器
        this.errorHandler = new SimpleYLibErrorHandler(this.loggerService);
        
        // 初始化命令管理器
        this.commandManager = new SimpleCommandManager(plugin, this.loggerService, this.messageService);
        
        // 注册核心服务到容器
        registerCoreServices();
            
            getLogger().info("核心服务初始化完成");
        } catch (Exception e) {
            throw new YLibException("核心服务初始化失败", e);
        }
    }
    
    /**
     * 初始化平台特定组件
     * 子类需要实现此方法来初始化调度器管理器和命令管理器
     * 
     * @throws YLibException 如果初始化失败
     */
    protected abstract void initializePlatformSpecific() throws YLibException;
    
    /**
     * 启用YLib实例
     */
    @Override
    public void enable() {
        if (enabled.compareAndSet(false, true)) {
            try {
                loggerService.startup("启用YLib " + getYLibVersion() + " (" + serverType.getDisplayName() + ")");
                
                // 执行启用逻辑
                onEnable();
                
                loggerService.startup("YLib启用完成");
            } catch (Exception e) {
                getLogger().severe("YLib启用失败: " + e.getMessage());
                enabled.set(false);
                throw new RuntimeException("YLib启用失败", e);
            }
        }
    }
    
    /**
     * 禁用YLib实例
     */
    @Override
    public void disable() {
        if (enabled.compareAndSet(true, false)) {
            try {
                loggerService.shutdown("正在禁用YLib...");
                
                // 执行禁用逻辑
                onDisable();
                
                // 清理服务容器
                if (serviceContainer != null) {
                    serviceContainer.clear();
                }
                
                loggerService.shutdown("YLib禁用完成");
            } catch (Exception e) {
                getLogger().severe("YLib禁用时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 启用时的具体逻辑，子类可以重写
     */
    protected void onEnable() {
        // 子类可以重写此方法添加启用逻辑
    }
    
    /**
     * 禁用时的具体逻辑，子类可以重写
     */
    protected void onDisable() {
        // 取消所有任务
        if (schedulerManager != null) {
            try {
                schedulerManager.cancelAllTasks();
            } catch (Exception e) {
                getLogger().warning("取消任务时发生错误: " + e.getMessage());
            }
        }
        
        // 注销所有命令
        if (commandManager != null) {
            try {
                // 注销所有已注册的命令
                String[] registeredCommands = commandManager.getRegisteredCommands();
                for (String commandName : registeredCommands) {
                    commandManager.unregisterCommand(commandName);
                }
            } catch (Exception e) {
                getLogger().warning("注销命令时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 重载配置
     */
    @Override
    public boolean reload() {
        try {
            getLogger().info("正在重载YLib配置...");
            
            // 重载配置服务
            configurationService.reloadConfig();
            
            // 执行平台特定的重载逻辑
            onReload();
            
            getLogger().info("YLib配置重载完成");
            return true;
        } catch (Exception e) {
            getLogger().severe("重载配置失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 重载时的具体逻辑，子类可以重写
     */
    protected void onReload() {
        // 子类可以重写此方法添加重载逻辑
    }
    
    // ========== Getter方法 ==========
    
    @Override
    @NotNull
    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }
    
    @Override
    @NotNull
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    @Override
    @NotNull
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }
    
    @Override
    @NotNull
    public LoggerService getLoggerService() {
        return loggerService;
    }
    
    @Override
    @NotNull
    public MessageService getMessageService() {
        return messageService;
    }
    
    @Override
    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    @Override
    @NotNull
    public String getPluginName() {
        return plugin.getName();
    }
    
    @Override
    @NotNull
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    @NotNull
    public String getPluginPrefix() {
        return "[" + getPluginName() + "]";
    }
    
    @Override
    @NotNull
    public String getYLibVersion() {
        return "1.0.0-beta5";
    }
    
    @Override
    @NotNull
    public ServerType getServerType() {
        return serverType;
    }
    
    @Override
    public boolean isFolia() {
        return serverType == ServerType.FOLIA;
    }
    
    @Override
    public boolean isPaper() {
        return serverType == ServerType.FOLIA || serverType == ServerType.PAPER;
    }
    
    @Override
    public boolean isSpigot() {
        return serverType == ServerType.FOLIA || 
               serverType == ServerType.PAPER || 
               serverType == ServerType.SPIGOT;
    }
    
    @Override
    @NotNull
    public String getServerVersion() {
        return ServerType.getServerVersion();
    }
    
    @Override
    @NotNull
    public String getBukkitVersion() {
        return ServerType.getBukkitVersion();
    }
    
    @Override
    public boolean isEnabled() {
        return enabled.get();
    }
    
    @Override
    @NotNull
    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }
    
    @Override
    @NotNull
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
    
    @Override
    @NotNull
    public YLibErrorHandler getErrorHandler() {
        return errorHandler;
    }
    
    /**
     * 注册核心服务到服务容器
     */
    private void registerCoreServices() {
        // 注册YLib API接口服务
        serviceContainer.registerSingleton(cn.yvmou.ylib.api.services.LoggerService.class, loggerService);
        serviceContainer.registerSingleton(cn.yvmou.ylib.api.services.ConfigurationService.class, configurationService);
        serviceContainer.registerSingleton(cn.yvmou.ylib.api.services.MessageService.class, messageService);
        serviceContainer.registerSingleton(ConfigurationManager.class, configurationManager);
        serviceContainer.registerSingleton(YLibErrorHandler.class, errorHandler);
        serviceContainer.registerSingleton(CommandManager.class, commandManager);
        
        // 注册实现类服务（向后兼容）
        serviceContainer.registerSingleton(LoggerService.class, loggerService);
        serviceContainer.registerSingleton(ConfigurationService.class, configurationService);
        serviceContainer.registerSingleton(MessageService.class, messageService);
        serviceContainer.registerSingleton(SimpleConfigurationManager.class, (SimpleConfigurationManager) configurationManager);
        serviceContainer.registerSingleton(SimpleYLibErrorHandler.class, (SimpleYLibErrorHandler) errorHandler);
        serviceContainer.registerSingleton(SimpleCommandManager.class, (SimpleCommandManager) commandManager);
        
        // 注册插件实例
        serviceContainer.registerSingleton(JavaPlugin.class, plugin);
        
        // 注册YLib API实例本身
        serviceContainer.registerSingleton(YLibAPI.class, this);
        
        getLogger().fine("核心服务已注册到服务容器");
    }
    
    /**
     * 获取日志记录器的便捷方法
     * 
     * @return 日志记录器
     */
    @NotNull
    protected java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }
}