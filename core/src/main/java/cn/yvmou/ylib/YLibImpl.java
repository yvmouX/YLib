package cn.yvmou.ylib;

import cn.yvmou.ylib.api.YLib;
import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.SimpleCommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.api.services.ServerInfoService;
import cn.yvmou.ylib.exception.YLibException;
import cn.yvmou.ylib.impl.command.SimpleCommandManagerImpl;
import cn.yvmou.ylib.impl.config.SimpleConfigurationManager;
import cn.yvmou.ylib.impl.services.LoggerServiceImpl;
import cn.yvmou.ylib.impl.services.MessageServiceImpl;
import cn.yvmou.ylib.impl.services.ServerInfoServiceImpl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class YLibImpl implements YLib {
    private static final Logger LOGGER = Logger.getLogger("YLib");
    // 插件实例
    // Plugin instance
    private final JavaPlugin plugin;
    // 服务实例
    // Server instance
    private ServerInfoService serverInfo;
    private CommandConfig commandConfig;
    private LoggerService loggerService;
    private MessageService messageService;
    private UniversalScheduler universalScheduler;
    private SimpleCommandManager simpleCommandManager;
    private ConfigurationManager configurationManager;

    public YLibImpl(@NotNull JavaPlugin plugin) throws YLibException {
        this.plugin = plugin;

        // 初始化核心服务
        initializeServices();
    }

    protected void initializeServices() throws YLibException {
        try {
            // 初始化服务器信息服务
            this.serverInfo = new ServerInfoServiceImpl(plugin);
            // 初始化日志服务
            this.loggerService = new LoggerServiceImpl(plugin);
            // 初始化消息服务
            this.messageService = new MessageServiceImpl(plugin, loggerService);
            // 初始化配置管理器
            this.configurationManager = new SimpleConfigurationManager(plugin, loggerService);
            // 初始化命令管理器
            this.simpleCommandManager = new SimpleCommandManagerImpl(plugin, getScheduler(), loggerService, messageService, serverInfo, commandConfig);

            LOGGER.info("核心服务初始化完成");
        } catch (Exception e) {
            throw new YLibException("核心服务初始化失败", e);
        }
    }

    // ========== 实现方法 ==========
    @Override
    @NotNull
    public UniversalScheduler getScheduler() {
        if (universalScheduler == null) {
            try {
                if (getServerInfo().isFolia()) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.FoliaScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else if (getServerInfo().isPaper()) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.PaperScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.SpigotScheduler");
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
    public SimpleCommandManager getSimpleCommandManager() {
        return simpleCommandManager;
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
