package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class YLib {
    private static YLib instance;

    public static YLib getyLib() {
        if (instance == null) {
            throw new IllegalStateException("YLib has not been initialized yet");
        }
        return instance;
    }

    public static void init(@NotNull JavaPlugin plugin) throws YLibException {
        if (instance == null) {
            instance = new YLib(plugin);
        }
    }

    // Plugin instance
    private final Plugin plugin;
    private final ServerType serverType;
    // Server instance
    private UniversalScheduler universalScheduler;
    private CommandManager commandManager;
    private ConfigurationManager configurationManager;
    private Logger logger;

    private static final String FOLIA_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.FoliaScheduler";
    private static final String PAPER_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.PaperScheduler";
    private static final String SPIGOT_SCHEDULER_CLASS = "cn.yvmou.ylib.scheduler.SpigotScheduler";
    private static final String CONFIGURATION_MANAGER_CLASS = "cn.yvmou.ylib.config.ConfigurationManagerImpl";
    private static final String COMMAND_MANAGER_CLASS = "cn.yvmou.ylib.command.CommandManagerImpl";
    private static final String LOGGER_CLASS = "cn.yvmou.ylib.logger.LoggerImpl";

    private YLib(@NotNull JavaPlugin plugin) throws YLibException {
        this.plugin = plugin;
        serverType = ServerType.detectServerType();

        initializeServices();
    }

    @NotNull
    public UniversalScheduler getScheduler() {
        if (universalScheduler == null) {
            try {
                Class<?> clazz = Class.forName(getSchedulerClassName());
                universalScheduler = (UniversalScheduler) clazz.getConstructor(Plugin.class).newInstance(plugin);
            } catch (ClassNotFoundException e) {
                throw new YLibException("Scheduler implementation not found. Please ensure the platform-specific module is included.", e);
            } catch (Exception e) {
                throw new YLibException("Failed to instantiate scheduler", e);
            }
        }
        return universalScheduler;
    }

    @NotNull
    public CommandManager getCommandManager() {
        return commandManager;
    }

    @NotNull
    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }

    // ========= 日志服务 ==========
    public Logger getLogger() {
        if (logger == null) {
            try {
                Class<?> clazz = Class.forName(LOGGER_CLASS);
                logger = (Logger) clazz.getConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                throw new YLibException("Logger implementation not found.", e);
            } catch (Exception e) {
                throw new YLibException("Failed to instantiate logger", e);
            }
        }
        return logger;
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

    private void initializeServices() throws YLibException {
        // Plugin Info
        try {
            PluginInfo.pluginName = plugin.getDescription().getName();
            PluginInfo.pluginPrefix = "§8[§b§l§n" + plugin.getDescription().getPrefix() + "§8]§r ";
            PluginInfo.pluginVersion = plugin.getDescription().getVersion();
            PluginInfo.loggerDebug = true;
            PluginInfo.loggerPrefix = plugin.getDescription().getName();
        } catch (Exception e) {
            throw new YLibException("Failed to retrieve plugin information.", e);
        }
        // 初始化配置管理器
        try {
            Class<?> clazz = Class.forName(CONFIGURATION_MANAGER_CLASS);
            this.configurationManager = (ConfigurationManager) clazz.getConstructor(Plugin.class, Logger.class).newInstance(plugin, getLogger());
        } catch (Exception e) {
            throw new YLibException("Failed to instantiate ConfigurationManager.", e);
        }
        // 初始化命令管理器
        try {
            Class<?> clazz = Class.forName(COMMAND_MANAGER_CLASS);
            this.commandManager = (CommandManager) clazz.getConstructor(Plugin.class, Logger.class).newInstance(plugin, getLogger());
        } catch (Exception e) {
            throw new YLibException("Failed to instantiate CommandManager.", e);
        }
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

}
