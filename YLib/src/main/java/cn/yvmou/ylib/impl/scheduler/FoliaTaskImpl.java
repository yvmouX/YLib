package cn.yvmou.ylib.impl.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

public class FoliaTaskImpl implements UniversalTask {
    private final ScheduledTask task;

    public FoliaTaskImpl(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public Plugin getOwningPlugin() {
        return task.getOwningPlugin();
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public boolean isRepeatingTask() {
        return task.isRepeatingTask();
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
}
