package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.ServerType;
import org.bukkit.plugin.Plugin;

/**
 * Spigot 平台的调度器提供器
 */
public class SpigotSchedulerProvider implements UniversalSchedulerProvider {

    @Override
    public ServerType getServerType() {
        return ServerType.SPIGOT;
    }

    @Override
    public UniversalScheduler create(Plugin plugin) {
        return new SpigotScheduler(plugin);
    }
}
