package cn.yvmou.ylib.paper.task;

import cn.yvmou.ylib.api.scheduler.Task;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Paper任务实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class PaperTask implements Task {
    
    private final BukkitTask bukkitTask;
    
    /**
     * 构造函数
     * @param bukkitTask Bukkit任务
     */
    public PaperTask(@NotNull BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }
    
    @Override
    public void cancel() {
        bukkitTask.cancel();
    }
    
    @Override
    public boolean isCancelled() {
        return bukkitTask.isCancelled();
    }
    
    @Override
    public int getTaskId() {
        return bukkitTask.getTaskId();
    }
    
    @Override
    public boolean isRunning() {
        return !bukkitTask.isCancelled();
    }
    
    @Override
    public long getDelay() {
        return 0; // Paper任务不直接提供延迟信息
    }
    
    @Override
    public long getPeriod() {
        // BukkitTask没有getPeriod方法，返回0
        return 0;
    }
    
    @Override
    public TaskType getType() {
        if (bukkitTask.isSync()) {
            return TaskType.SYNC;
        } else {
            return TaskType.ASYNC;
        }
    }
} 