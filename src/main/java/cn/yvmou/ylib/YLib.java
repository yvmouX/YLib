package cn.yvmou.ylib;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.impl.scheduler.BukkitSchedulerImpl;
import cn.yvmou.ylib.impl.scheduler.FoliaSchedulerImpl;
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

    /**
     * 获取实例
     *
     * @return {@link YLib }
     */
    public Plugin getInstance() {
        if (plugin == null) {
            throw new IllegalStateException("This plugin hasn't been loaded yet!");
        }
        return plugin;
    }

    /**
     * 是叶
     *
     * @return boolean
     */
    public boolean isFolia() {
        return YlibR.isFolia();
    }

    /**
     * 获取调度程序
     *
     * @return {@link UniversalScheduler }
     */
    public UniversalScheduler getScheduler() {
        if (isFolia()) {
            return new FoliaSchedulerImpl(plugin, Bukkit.getGlobalRegionScheduler(), Bukkit.getRegionScheduler(), Bukkit.getAsyncScheduler());
        } else return new BukkitSchedulerImpl(plugin);
    }
}
