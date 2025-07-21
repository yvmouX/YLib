package cn.yvmou.ylib;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.impl.command.CommandConfig;
import cn.yvmou.ylib.impl.command.CommandManager;
import cn.yvmou.ylib.impl.scheduler.BukkitSchedulerImpl;
import cn.yvmou.ylib.impl.scheduler.FoliaSchedulerImpl;
import cn.yvmou.ylib.tools.ConfigTools;
import cn.yvmou.ylib.tools.LoggerTools;
import cn.yvmou.ylib.tools.MessageTools;
import cn.yvmou.ylib.tools.SchedulerTools;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ylib主类
 *
 * @author yvmoux
 * &#064;date  2025/07/19
 */
public class YLib {
    public static YLib yLib;
    private final JavaPlugin plugin;
    public String name;
    public String prefix;
    public String version;

    public YLib(JavaPlugin plugin) {
        this.plugin = plugin;
        yLib = this;
        setPluginPrefix();
        setPluginVersion();
        setPluginName();
    }

    private void setPluginPrefix() {
        if (plugin.getDescription().getPrefix() == null) {
            prefix = plugin.getDescription().getName();
        } else {
            prefix = plugin.getDescription().getPrefix();
        }
    }

    private void setPluginVersion() {
        version = plugin.getDescription().getVersion();
    }

    private void setPluginName() {
        name = plugin.getDescription().getName();
    }


    public static YLib getyLib() {
        return yLib;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPluginName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Plugin getInstance() {
        if (plugin == null) {
            throw new IllegalStateException("This plugin hasn't been loaded yet!");
        }
        return plugin;
    }

    public UniversalScheduler getScheduler() {
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
        if (isFolia) {
            return new FoliaSchedulerImpl(plugin, Bukkit.getGlobalRegionScheduler(), Bukkit.getRegionScheduler(), Bukkit.getAsyncScheduler());
        } else return new BukkitSchedulerImpl(plugin);
    }

    public CommandManager getCommandManager() {
        return new CommandManager(plugin);
    }

    public CommandConfig getCommandConfig() {
        return new CommandConfig(plugin);
    }

    // tools

    public SchedulerTools getSchedulerDogTools() {
        return new SchedulerTools(getScheduler());
    }

    public ConfigTools getConfigTools() {
        return new ConfigTools(plugin, new LoggerTools(plugin));
    }

    public LoggerTools getLoggerTools() {
        return new LoggerTools(plugin);
    }

    public MessageTools getMessageTools() {
        return new MessageTools(plugin);
    }
}
