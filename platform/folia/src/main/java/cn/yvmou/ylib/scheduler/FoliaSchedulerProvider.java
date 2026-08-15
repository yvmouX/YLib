package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.ServerType;
import org.bukkit.plugin.Plugin;

/**
 * Folia 平台的调度器提供器
 */
public class FoliaSchedulerProvider implements UniversalSchedulerProvider {

    @Override
    public ServerType getServerType() {
        return ServerType.FOLIA;
    }

    @Override
    public UniversalScheduler create(Plugin plugin) {
        return new FoliaScheduler(plugin);
    }
}
