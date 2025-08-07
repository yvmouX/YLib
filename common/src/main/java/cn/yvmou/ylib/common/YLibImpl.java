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
import cn.yvmou.ylib.common.utils.ValidationUtils;
import org.bukkit.Bukkit;
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
public abstract class YLibImpl implements YLib {
    public static YLibImpl ylib;
    // 内部实例
    protected final CommandConfig commandConfig;

    // 插件实例
    protected final JavaPlugin plugin;

    // 服务实例
    protected LoggerService loggerService;
    protected ConfigurationService configurationService;
    protected MessageService messageService;
    // 调度器实例
    protected UniversalScheduler universalScheduler;
    // 命令管理实例
    protected CommandManager commandManager;
    //
    protected ConfigurationManager configurationManager;

    protected static final Logger LOGGER = Logger.getLogger("YLib");
    /**
     * 构造函数
     *
     * @param plugin     插件实例
     * @throws YLibException 如果初始化失败
     */
    protected YLibImpl(@NotNull JavaPlugin plugin) throws YLibException {
        ValidationUtils.notNull(plugin, "插件实例不能为null");

        ylib = this;
        this.commandConfig = new CommandConfig(plugin, new LoggerService(plugin));
        this.plugin = plugin;

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
                if (isFolia()) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.folia.scheduler.FoliaScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else if (isPaper()) {
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
        if (plugin.getDescription().getPrefix() != null) {
            return plugin.getDescription().getPrefix();
        }
        return "[" + getPluginName() + "]";
    }
    
    @Override
    @NotNull
    public String getYLibVersion() {
        return "1.0.0-beta5";
    }
    
    @Override
    @NotNull
    public String getServerType() {
        if (isFolia()) {
            return "FOLIA";
        } else if (isPaper()) {
            return "PAPER";
        } else if (isSpigot()) {
            return "SPIGOT";
        } else {
            return "UNKNOWN";
        }
    }
    
    @Override
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    @NotNull
    public String getServerVersion() {
        try {
            return Bukkit.getVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    @Override
    @NotNull
    public String getBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    @Override
    @NotNull
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }



}