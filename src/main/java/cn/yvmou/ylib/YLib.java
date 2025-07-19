package cn.yvmou.ylib;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.impl.scheduler.BukkitSchedulerImpl;
import cn.yvmou.ylib.impl.scheduler.FoliaSchedulerImpl;
import cn.yvmou.ylib.tools.ConfigTools;
import cn.yvmou.ylib.tools.LoggerTools;
import cn.yvmou.ylib.tools.MessageTools;
import cn.yvmou.ylib.tools.SchedulerTools;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * ylib主类
 *
 * @author yvmoux
 * &#064;date  2025/07/19
 */
public class YLib {
    private final Plugin plugin;

    public YLib(Plugin plugin) {
        this.plugin = plugin;
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
