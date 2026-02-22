package cn.yvmou.ylib;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.enums.ServerType;
import cn.yvmou.ylib.exception.YLibException;
import cn.yvmou.ylib.impl.command.CommandManagerImpl;
import cn.yvmou.ylib.impl.config.ConfigurationManagerImpl;
import cn.yvmou.ylib.impl.logger.LoggerImpl;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class YLib {

    // Plugin instance
    private final JavaPlugin plugin;
    private final ServerType serverType;
    // Server instance
    private UniversalScheduler universalScheduler;
    private CommandManager commandManager;
    private ConfigurationManager configurationManager;

    private static final String FOLIA_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.FoliaScheduler";
    private static final String PAPER_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.PaperScheduler";
    private static final String SPIGOT_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.SpigotScheduler";

    // 内部静态变量
    private static UniversalScheduler globalScheduler;
    private static Logger logger;

    public YLib(@NotNull JavaPlugin plugin) throws YLibException {
        this(plugin, ServerType.detectServerType());
    }

    public YLib(@NotNull JavaPlugin plugin, ServerType serverType) throws YLibException {
        this.plugin = plugin;
        this.serverType = serverType;

        // 初始化核心服务
        initializeServices();
    }

    protected void initializeServices() throws YLibException {
        try {
            // Plugin Info
            PluginInfo.pluginName = plugin.getDescription().getName();
            PluginInfo.pluginPrefix = "§8[§b§l§n" + plugin.getDescription().getPrefix() + "§8]§r ";
            PluginInfo.pluginVersion = plugin.getDescription().getVersion();
            PluginInfo.loggerDebug = true;
            PluginInfo.loggerPrefix = plugin.getDescription().getName();

            // 初始化配置管理器
            this.configurationManager = new ConfigurationManagerImpl(plugin, getLogger());
            // 初始化命令管理器
            this.commandManager = new CommandManagerImpl(plugin, getLogger());
            
            // 初始化内部静态变量
            globalScheduler = getScheduler();
            logger = getLogger();
        } catch (Exception e) {
            throw new YLibException("核心服务初始化失败", e);
        }
    }

    // 内部方法
    public static UniversalScheduler _getGlobalScheduler() {
        return globalScheduler;
    }
    public static Logger _getLogger() {
        return logger;
    }

    // ========== 实现方法 ==========
    @NotNull
    public UniversalScheduler getScheduler() {
        if (universalScheduler == null) {
            try {
                final String schedulerClassName = getSchedulerClassName();

                Class<?> schedulerClass = Class.forName(schedulerClassName);
                universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                        .newInstance(plugin);
            } catch (ClassNotFoundException e) {
                throw new YLibException("Scheduler implementation not found. Please ensure the platform-specific module is included.", e);
            } catch (Exception e) {
                throw new YLibException("Failed to instantiate scheduler", e);
            }
        }
        return universalScheduler;
    }

    private @NotNull String getSchedulerClassName() {
        String schedulerClassName;
        if (serverType == ServerType.FOLIA) {
            schedulerClassName = FOLIA_SCHEDULER_CLASS;
        } else if (serverType == ServerType.PAPER) {
            schedulerClassName = PAPER_SCHEDULER_CLASS;
        } else if (serverType == ServerType.SPIGOT) {
            schedulerClassName = SPIGOT_SCHEDULER_CLASS;
        } else {
            throw new YLibException("Unsupported server type: " + serverType);
        }
        return schedulerClassName;
    }

    @NotNull
    public CommandManager getCommandManager() {
        return commandManager;
    }


    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * 获取配置管理器
     * <p>
     * 配置管理器提供基于注解的自动配置功能，支持约定优于配置的理念。
     * 让配置管理变得更加简单和智能。
     * </p>
     *
     * <p>主要功能：</p>
     * <ul>
     *   <li>自动扫描和加载配置类</li>
     *   <li>自动生成默认配置文件</li>
     *   <li>配置值验证和类型转换</li>
     *   <li>配置热重载</li>
     *   <li>配置变更监听</li>
     * </ul>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 注册配置类
     * DatabaseConfig dbConfig = ylib.getConfigurationManager().registerConfiguration(DatabaseConfig.class);
     *
     * // 使用配置
     * String host = dbConfig.getHost();
     * int port = dbConfig.getPort();
     *
     * // 监听配置变更
     * ylib.getConfigurationManager().addConfigurationListener(DatabaseConfig.class, (oldConfig, newConfig) -> {
     *     // 重新连接数据库
     *     reconnectDatabase(newConfig);
     * });
     * }</pre>
     *
     * @return 配置管理器实例
     */
    @NotNull
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    // ========= 日志服务 ==========
    public Logger getLogger() {
        return new LoggerImpl();
    }

    // ========= 插件信息 ==========
    public String getPluginName() {
        return PluginInfo.getPluginName();
    }

    public String getPluginPrefix() {
        return PluginInfo.getPluginPrefix();
    }

    public String getPluginVersion() {
        return PluginInfo.getPluginVersion();
    }
}