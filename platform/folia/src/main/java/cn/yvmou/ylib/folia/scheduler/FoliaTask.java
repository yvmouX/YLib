package cn.yvmou.ylib.folia.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

/**
 * Folia任务实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class FoliaTask implements UniversalTask {
    
    private final ScheduledTask task;
    
    /**
     * 构造函数
     * @param task Folia调度任务
     */
    public FoliaTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public Plugin getOwningPlugin() {
        return task.getOwningPlugin();
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
        final ScheduledTask.ExecutionState state = task.getExecutionState();
        // The task is considered "currently running" if:
        // - It is actively running (RUNNING), or
        // - It is running but future executions are cancelled (CANCELLED_RUNNING)
        // 如果出现以下情况，任务将被视为 "正在运行"：
        // 正在运行（RUNNING），或正在运行，但未来的执行被取消（CANCELLED_RUNNING）
        return state == ScheduledTask.ExecutionState.RUNNING || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
    }
    
    @Override
    public TaskType getType() {
        if (task.isRepeatingTask()) {
            return TaskType.REPEATING;
        }
        if (isAsync()) {
            return TaskType.ASYNC;
        }
        return TaskType.SYNC;
    }

    /**
     * 判断任务是否为异步任务
     * 在Folia中，异步任务的类名中会包含"Async"字样
     * @return boolean 如果是异步任务返回true
     */
    private boolean isAsync() {
        try {
            String className = task.getClass().getName();
            return className.contains("Async") || className.contains("async");
        } catch (Exception e) {
            return false;
        }
    }
}