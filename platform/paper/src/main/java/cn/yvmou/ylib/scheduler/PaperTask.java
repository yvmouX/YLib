package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Paper任务实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class PaperTask implements UniversalTask {

    private final BukkitTask task;

    boolean isRepeating;

    /**
     * 构造函数
     * @param task Bukkit任务
     */
    public PaperTask(BukkitTask task) {
        this.task = task;
    }

    public PaperTask(BukkitTask task, boolean isRepeating) {
        this.task = task;
        this.isRepeating = isRepeating;
    }

    @Override
    public Plugin getOwningPlugin() {
        return task.getOwner();
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public boolean isCurrentlyRunning() {
        return Bukkit.getServer().getScheduler().isCurrentlyRunning(task.getTaskId()) &&
                Bukkit.getServer().getScheduler().isQueued(task.getTaskId());
    }

    @Override
    public TaskType getType() {
        if (isRepeating) {
            return TaskType.REPEATING;
        }
        if (task.isSync()) {
            return TaskType.SYNC;
        }
        return TaskType.ASYNC;
    }
}
