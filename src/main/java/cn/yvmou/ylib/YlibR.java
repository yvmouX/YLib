package cn.yvmou.ylib;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.impl.scheduler.BukkitSchedulerImpl;
import cn.yvmou.ylib.impl.scheduler.FoliaSchedulerImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class YlibR extends JavaPlugin {
    private static YlibR instance;

    public YlibR() {
        instance = this;
    }

    /**
     * 获取实例
     *
     * @return {@link YlibR }
     */
    public static YlibR getInstance() {
        if (instance == null) {
            throw new IllegalStateException("This plugin hasn't been loaded yet!");
        }
        return instance;
    }

    /**
     * 是叶
     *
     * @return boolean
     */
    public static boolean isFolia() {
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
        return isFolia;
    }

    /**
     * 获取调度程序
     *
     * @return {@link UniversalScheduler }
     */
    public static UniversalScheduler getScheduler() {
        if (isFolia()) {
            return new FoliaSchedulerImpl(instance, Bukkit.getGlobalRegionScheduler(), Bukkit.getRegionScheduler(), Bukkit.getAsyncScheduler());
        } else return new BukkitSchedulerImpl(instance);
    }
}
