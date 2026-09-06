package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.ServerType;
import org.bukkit.plugin.Plugin;

/**
 * Canvas 平台的调度器提供器
 */
public class CanvasSchedulerProvider implements UniversalSchedulerProvider {

    @Override
    public ServerType getServerType() {
        return ServerType.CANVAS;
    }

    @Override
    public UniversalScheduler create(Plugin plugin) {
        return new CanvasScheduler(plugin);
    }
}
