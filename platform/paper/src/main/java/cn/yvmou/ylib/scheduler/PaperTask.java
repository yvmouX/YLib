package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Paper任务实现（Paper 与 Spigot 的 BukkitTask 行为一致，直接复用 SpigotTask）
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class PaperTask extends SpigotTask {

    public PaperTask(BukkitTask task) {
        super(task);
    }

    public PaperTask(BukkitTask task, boolean isRepeating) {
        super(task, isRepeating);
    }
}
