package cn.yvmou.ylib.api.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

/**
 * 通用任务
 *
 * @author admin
 * &#064;date  2025/07/19
 */
public interface UniversalTask {
    /**
     * 获取拥有插件
     *
     * @return {@link Plugin }
     */
    Plugin getOwningPlugin();

    /**
     * 被取消
     *
     * @return boolean
     */
    boolean isCancelled();

    /**
     * 取消
     */
    void cancel();

    /**
     * 正在重复任务
     *  bukkit: 注意：此方法不100%可靠，因为一次性任务也可能短暂处于"queued"状态
     *
     * @return boolean
     */
    boolean isRepeatingTask();

    /**
     * Checks whether this task is currently executing (running) at the time of the call.
     * <p>
     * This method returns {@code true} if the task is actively running, regardless of whether
     * it has been cancelled for future executions (in the case of repeating tasks).
     * </p>
     *
     * @return {@code true} if the task is in {@link ScheduledTask.ExecutionState#RUNNING} or
     *         {@link ScheduledTask.ExecutionState#CANCELLED_RUNNING}, meaning it is currently executing;
     *         {@code false} otherwise (idle, finished, or fully cancelled).
     */
    boolean isCurrentlyRunning();
}
