package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.ServerType;
import org.bukkit.plugin.Plugin;

/**
 * 平台调度器工厂接口。
 * <p>
 * 每个平台模块（folia/paper/spigot）通过
 * {@code META-INF/services/cn.yvmou.ylib.scheduler.UniversalSchedulerProvider} 注册各自的实现，
 * YLib 根据 {@link ServerType} 选择匹配的提供器创建调度器。
 */
public interface UniversalSchedulerProvider {

    ServerType getServerType();

    UniversalScheduler create(Plugin plugin);
}
