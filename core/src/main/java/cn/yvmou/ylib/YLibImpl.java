package cn.yvmou.ylib;

import cn.yvmou.ylib.api.YLib;
import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.api.services.ServerInfoService;
import cn.yvmou.ylib.enums.LoggerOption;
import cn.yvmou.ylib.exception.YLibException;
import cn.yvmou.ylib.impl.command.CommandConfigImpl;
import cn.yvmou.ylib.impl.command.CommandManagerImpl;
import cn.yvmou.ylib.impl.config.ConfigurationManagerImpl;
import cn.yvmou.ylib.impl.logger.LoggerImpl;
import cn.yvmou.ylib.impl.services.MessageServiceImpl;
import cn.yvmou.ylib.impl.services.ServerInfoServiceImpl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class YLibImpl implements YLib {
    // Plugin instance
    private final JavaPlugin plugin;
    // 服务实例
    // Server instance
    private ServerInfoService serverInfo;
    private CommandConfig commandConfig;
    private MessageService messageService;
    private UniversalScheduler universalScheduler;
    private CommandManager commandManager;
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
            // 初始化消息服务
            this.messageService = new MessageServiceImpl(plugin, createLogger());
            // 初始化配置管理器
            this.configurationManager = new ConfigurationManagerImpl(plugin, createLogger());
            // 初始化命令配置管理器
            this.commandConfig = new CommandConfigImpl(plugin, createLogger());
            // 初始化命令管理器
            this.commandManager = new CommandManagerImpl(plugin, getScheduler(), createLogger(), messageService, serverInfo, commandConfig);
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

    // ========= 日志服务 ==========
    @Override
    public LoggerImpl createLogger() {
        return new LoggerImpl(getConsolePrefix());
    }

    @Override
    public LoggerImpl createLogger(@NotNull LoggerOption option) {
        return new LoggerImpl(getConsolePrefix(), option);
    }

    @Override
    public LoggerImpl createLogger(@NotNull String prefix) {
        return new LoggerImpl(prefix);
    }

    @Override
    public LoggerImpl createLogger(@NotNull String prefix, @NotNull LoggerOption option) {
        return new LoggerImpl(prefix, option);
    }

    @Override
    public String getConsolePrefix() {
        String prefix = plugin.getDescription().getPrefix();
        String pluginName = (prefix == null ? "UNKNOW" : prefix);
        return "§8[§b§l§n" + pluginName + "§8]§r ";
    }
}