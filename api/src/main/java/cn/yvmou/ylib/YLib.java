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
            String implSimpleName;
            switch (serverType) {
                case FOLIA:
                    implSimpleName = "FoliaSchedulerProvider";
                    break;
                case PAPER:
                    implSimpleName = "PaperSchedulerProvider";
                    break;
                case SPIGOT:
                    implSimpleName = "SpigotSchedulerProvider";
                    break;
                default:
                    implSimpleName = null;
            }

            Throwable failure = null;

            // 1) 标准 ServiceLoader 查找
            try {
                ServiceLoader<UniversalSchedulerProvider> loader = ServiceLoader.load(UniversalSchedulerProvider.class);
                Iterator<UniversalSchedulerProvider> it = loader.iterator();
                while (it.hasNext()) {
                    try {
                        UniversalSchedulerProvider provider = it.next();
                        if (provider.getServerType() == serverType) {
                            universalScheduler = provider.create(plugin);
                            break;
                        }
                    } catch (ServiceConfigurationError error) {
                        if (failure == null) failure = error;
                    }
                }
            } catch (Throwable throwable) {
                if (failure == null) failure = throwable;
            }

            // 2) 重定位安全回退：接口所在包 + 实现类简单名，
            //    使用加载接口的类加载器（插件自己的），不依赖线程上下文类加载器
            if (universalScheduler == null && implSimpleName != null) {
                try {
                    Package pkg = UniversalSchedulerProvider.class.getPackage();
                    if (pkg != null) {
                        Class<?> impl = Class.forName(pkg.getName() + "." + implSimpleName, true, UniversalSchedulerProvider.class.getClassLoader());
                        UniversalSchedulerProvider provider = (UniversalSchedulerProvider) impl.getDeclaredConstructor().newInstance();
                        universalScheduler = provider.create(plugin);
                    }
                } catch (Throwable throwable) {
                    if (failure == null) failure = throwable;
                }
            }

            if (universalScheduler == null) {
                throw new YLibException("Scheduler implementation not found for server type: " + serverType
                        + ". Please ensure the platform-specific module is included. Cause: " + failure, failure);
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
        YLibServices services = ServiceLocator.locate(YLibServices.class, "CoreServices");

        this.logger = services.createLogger();
        this.configurationManager = services.createConfigurationManager(plugin, logger);
        this.commandManager = services.createCommandManager(plugin, logger);
    }

}
