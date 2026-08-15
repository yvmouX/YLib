package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.scheduler.UniversalTask;
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
    private final boolean async;

    /**
     * 构造函数
     * @param task Folia调度任务
     * @param async 是否为异步任务（由创建方明确指定，不依赖内部类名推断）
     */
    public FoliaTask(ScheduledTask task, boolean async) {
        this.task = task;
        this.async = async;
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
        if (async) {
            return TaskType.ASYNC;
        }
        return TaskType.SYNC;
    }
}
