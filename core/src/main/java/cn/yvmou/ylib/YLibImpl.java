package cn.yvmou.ylib;

import cn.yvmou.ylib.api.YLib;
import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.enums.LoggerOption;
import cn.yvmou.ylib.enums.ServerType;
import cn.yvmou.ylib.exception.YLibException;
import cn.yvmou.ylib.impl.command.CommandConfigImpl;
import cn.yvmou.ylib.impl.command.CommandManagerImpl;
import cn.yvmou.ylib.impl.config.ConfigurationManagerImpl;
import cn.yvmou.ylib.impl.logger.LoggerImpl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class YLibImpl implements YLib {
    // Plugin instance
    private final JavaPlugin plugin;
    private final ServerType serverType;
    // Server instance
    private CommandConfig commandConfig;
    private UniversalScheduler universalScheduler;
    private CommandManager commandManager;
    private ConfigurationManager configurationManager;

    public YLibImpl(@NotNull JavaPlugin plugin, ServerType serverType) throws YLibException {
        this.plugin = plugin;
        this.serverType = serverType;

        // 初始化核心服务
        initializeServices();
    }

    protected void initializeServices() throws YLibException {
        try {
            // Plugin Info
            PluginInfo.pluginName = plugin.getName();
            PluginInfo.pluginPrefix = "§8[§b§l§n" + plugin.getDescription().getPrefix() + "§8]§r ";
            PluginInfo.pluginVersion = plugin.getDescription().getVersion();
            // 初始化配置管理器
            this.configurationManager = new ConfigurationManagerImpl(plugin, createLogger());
            // 初始化命令配置管理器
            this.commandConfig = new CommandConfigImpl(plugin, createLogger());
            // 初始化命令管理器
            this.commandManager = new CommandManagerImpl(plugin, getScheduler(), createLogger(), commandConfig);
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
                if (serverType == ServerType.FOLIA) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.FoliaScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else if (serverType == ServerType.PAPER) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.PaperScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else if (serverType == ServerType.SPIGOT) {
                    Class<?> schedulerClass = Class.forName("cn.yvmou.ylib.scheduler.SpigotScheduler");
                    universalScheduler = (UniversalScheduler) schedulerClass.getConstructor(Plugin.class)
                            .newInstance(plugin);
                } else {
                    throw new YLibException("Unsupported server type: " + serverType);
                }
            } catch (Exception e) {
                throw new YLibException("Failed to get scheduler", e);
            }
        }
        return universalScheduler;
    }

    @Override
    @NotNull
    public CommandManager getCommandManager() {
        return commandManager;
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
        return new LoggerImpl(PluginInfo.getPluginPrefix());
    }

    @Override
    public LoggerImpl createLogger(@NotNull LoggerOption option) {
        return new LoggerImpl(PluginInfo.getPluginPrefix(), option);
    }

    @Override
    public LoggerImpl createLogger(@NotNull String prefix) {
        return new LoggerImpl(prefix);
    }

    @Override
    public LoggerImpl createLogger(@NotNull String prefix, @NotNull LoggerOption option) {
        return new LoggerImpl(prefix, option);
    }

    // ========= 插件信息 ==========
    @Override
    public String getPluginName() {
        return PluginInfo.getPluginName();
    }

    @Override
    public String getPluginPrefix() {
        return PluginInfo.getPluginPrefix();
    }

    @Override
    public String getPluginVersion() {
        return PluginInfo.getPluginVersion();
    }
}