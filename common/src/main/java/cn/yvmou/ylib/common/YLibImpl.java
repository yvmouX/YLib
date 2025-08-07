package cn.yvmou.ylib.common;


import cn.yvmou.ylib.api.YLib;
import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.common.command.CommandConfig;
import cn.yvmou.ylib.common.command.SimpleCommandManager;
import cn.yvmou.ylib.common.config.SimpleConfigurationManager;
import cn.yvmou.ylib.common.services.ConfigurationService;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.services.MessageService;
import cn.yvmou.ylib.common.services.ServerInfoService;
import cn.yvmou.ylib.common.utils.ValidationUtils;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * YLib抽象基类，提供所有平台实现的通用功能
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibImpl implements YLib {
    public static YLibImpl ylib;
    protected static final Logger LOGGER = Logger.getLogger("YLib");

    // 插件实例
    protected final JavaPlugin plugin;
    
    // 服务实例
    protected final ServerInfoService serverInfo;
    protected final CommandConfig commandConfig;
    protected LoggerService loggerService;
    protected ConfigurationService configurationService;
    protected MessageService messageService;
    protected UniversalScheduler universalScheduler;
    protected CommandManager commandManager;
    protected ConfigurationManager configurationManager;
    /**
     * 构造函数
     *
     * @param plugin     插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibImpl(@NotNull JavaPlugin plugin) throws YLibException {
        ValidationUtils.notNull(plugin, "插件实例不能为null");

        ylib = this;
        this.plugin = plugin;
        this.serverInfo = new ServerInfoService(plugin);
        this.commandConfig = new CommandConfig(plugin, new LoggerService(plugin));

        try {
            // 初始化核心服务
            initializeServices();

            LOGGER.info("YLib实例初始化完成");
        } catch (Exception e) {
            throw new YLibException("YLib初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化核心服务
     *
     * @throws YLibException 如果初始化失败
     */
    protected void initializeServices() throws YLibException {
        try {
            // 初始化日志服务
            this.loggerService = new LoggerService(plugin);

            // 初始化配置服务
            this.configurationService = new ConfigurationService(plugin, this.loggerService);

            // 初始化消息服务
            this.messageService = new MessageService(plugin, this.loggerService);

            // 初始化配置管理器
            this.configurationManager = new SimpleConfigurationManager(plugin, this.loggerService);

            // 初始化命令管理器
            this.commandManager = new SimpleCommandManager(plugin, this.loggerService, this.messageService);

            LOGGER.info("核心服务初始化完成");
        } catch (Exception e) {
            throw new YLibException("核心服务初始化失败", e);
        }
    }

    public CommandConfig getCommandConfig() {
        return commandConfig;
    }



    // ========== 实现方法 ==========
    @Override
    @NotNull
    public UniversalScheduler getScheduler() {
        // 懒加载
        if (universalScheduler == null) {
            try {
                if (getServerInfo().isFolia()) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.folia.scheduler.FoliaScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else if (getServerInfo().isPaper()) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.paper.scheduler.PaperScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.spigot.scheduler.SpigotScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                }
            } catch (Exception e) {
                LOGGER.severe("无法创建调度器: " + e.getMessage());
                throw new YLibException("无法创建调度器", e);
            }
        }
        return universalScheduler;
    }

    @Override
    @NotNull
    public ServerInfoService getServerInfo() { return serverInfo; }
    
    @Override
    @NotNull
    public CommandManager getSimpleCommandManager() {
        return commandManager;
    }
    
    @Override
    @NotNull
    public ConfigurationService getConfiguration() {
        return configurationService;
    }
    
    @Override
    @NotNull
    public LoggerService getSimpleLogger() {
        return loggerService;
    }
    
    @Override
    @NotNull
    public MessageService getMessages() {
        return messageService;
    }
    
    @Override
    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    @Override
    @NotNull
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }



}