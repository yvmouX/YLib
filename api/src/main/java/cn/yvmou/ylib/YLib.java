package cn.yvmou.ylib;

import cn.yvmou.ylib.command.CommandManager;
import cn.yvmou.ylib.config.ConfigurationManager;
import cn.yvmou.ylib.logger.Logger;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import cn.yvmou.ylib.scheduler.UniversalSchedulerProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class YLib {
    private static YLib instance;

    @NotNull
    public static YLib getYLib() {
        if (instance == null) {
            throw new IllegalStateException("YLib has not been initialized yet");
        }
        return instance;
    }

    /**
     * @deprecated 命名不统一，请使用 {@link #getYLib()}
     */
    @Deprecated
    @NotNull
    public static YLib getyLib() {
        return getYLib();
    }

    @NotNull
    public static YLib init(@NotNull JavaPlugin plugin) throws YLibException {
        if (instance == null) {
            instance = new YLib(plugin);
            return instance;
        }
        // YLib 是单例且服务都绑定在首个初始化它的插件上，
        // 其他插件重复初始化会导致命令/配置挂到错误的插件上，直接报错而不是静默复用
        if (!instance.plugin.equals(plugin)) {
            throw new YLibException("YLib has already been initialized by another plugin: "
                    + instance.plugin.getName());
        }
        return instance;
    }

    // Plugin instance
    private final Plugin plugin;
    private final ServerType serverType;
    // Server instance
    private UniversalScheduler universalScheduler;
    private CommandManager commandManager;
    private ConfigurationManager configurationManager;
    private Logger logger;

    private YLib(@NotNull JavaPlugin plugin) throws YLibException {
        this.plugin = plugin;
        serverType = ServerType.detectServerType();

        initializePluginInfo();
        initializeServices();
    }

    @NotNull
    public UniversalScheduler getScheduler() {
        if (universalScheduler == null) {
            ServiceLoader<UniversalSchedulerProvider> loader = ServiceLoader.load(UniversalSchedulerProvider.class);
            Iterator<UniversalSchedulerProvider> it = loader.iterator();
            while (it.hasNext()) {
                try {
                    UniversalSchedulerProvider provider = it.next();
                    if (provider.getServerType() == serverType) {
                        universalScheduler = provider.create(plugin);
                        break;
                    }
                } catch (ServiceConfigurationError ignored) {
                    // provider 类缺失（例如只打包了部分平台模块），跳过该条目继续尝试
                }
            }
            if (universalScheduler == null) {
                throw new YLibException("Scheduler implementation not found for server type: " + serverType
                        + ". Please ensure the platform-specific module is included.");
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

    private void initializePluginInfo() throws YLibException {
        // Plugin Info
        try {
            PluginInfo.setPluginName(plugin.getDescription().getName());
            String prefix = plugin.getDescription().getPrefix();
            if (prefix == null || prefix.isEmpty()) {
                prefix = plugin.getName();
            }
            PluginInfo.setPluginPrefix("§8[§b§l" + prefix + "§8]§r ");
            PluginInfo.setPluginVersion(plugin.getDescription().getVersion());
            PluginInfo.setLoggerPrefix(plugin.getDescription().getName());
        } catch (Exception e) {
            throw new YLibException("Failed to retrieve plugin information.", e);
        }
    }

    private void initializeServices() throws YLibException {
        YLibServices services = null;
        try {
            ServiceLoader<YLibServices> loader = ServiceLoader.load(YLibServices.class);
            Iterator<YLibServices> it = loader.iterator();
            while (it.hasNext()) {
                try {
                    services = it.next();
                    break;
                } catch (ServiceConfigurationError ignored) {
                    // provider 类缺失，跳过该条目继续尝试
                }
            }
        } catch (Exception e) {
            throw new YLibException("Failed to locate YLib core services.", e);
        }
        if (services == null) {
            throw new YLibException("YLib core services not found. Please ensure the core module is included.");
        }

        this.logger = services.createLogger();
        this.configurationManager = services.createConfigurationManager(plugin, logger);
        this.commandManager = services.createCommandManager(plugin, logger);
    }

}
