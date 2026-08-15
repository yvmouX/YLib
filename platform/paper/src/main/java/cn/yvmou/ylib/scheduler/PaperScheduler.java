package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Paper调度器管理器实现。
 * <p>
 * Paper（非 Folia）的调度行为与 Spigot 完全一致，全部复用 {@link SpigotScheduler}，
 * 仅异步传送使用 Paper API。
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class PaperScheduler extends SpigotScheduler implements UniversalScheduler {

    public PaperScheduler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void teleportAsync(Entity entity, Location location) {
        entity.teleportAsync(location);
    }
}
