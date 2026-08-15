package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.ServerType;
import org.bukkit.plugin.Plugin;

/**
 * Paper 平台的调度器提供器
 */
public class PaperSchedulerProvider implements UniversalSchedulerProvider {

    @Override
    public ServerType getServerType() {
        return ServerType.PAPER;
    }

    @Override
    public UniversalScheduler create(Plugin plugin) {
        return new PaperScheduler(plugin);
    }
}
