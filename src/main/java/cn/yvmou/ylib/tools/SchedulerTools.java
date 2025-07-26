package cn.yvmou.ylib.tools;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * 更方便的使Bukkit插件适配Folia
 * 懒狗专属
 */
public class SchedulerTools {
    private final UniversalScheduler s;

    public SchedulerTools(UniversalScheduler s) {
        this.s = s;
    }

    /**
     * 检查当前服务器是否运行在 Folia 环境下。
     *
     * @return 如果服务器运行在 Folia 环境下返回 true，否则返回 false。
     */
    public boolean isFolia() {

        return s.isFolia();
    }

    /**
     * 在当前服务器环境下运行任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param entity 目标实体（可选）
     * @param location 目标位置（可选）
     * @return 任务的封装结果
     */
    public UniversalTask runTask(Plugin plugin, Runnable runnable, Entity entity, Location location) {
        if (entity != null) {
            return s.runTask(entity, runnable);
        } else if (location != null) {
            return s.runTask(location, runnable);
        } else {
            return s.runTask(runnable);
        }
    }

    /**
     * 在当前服务器环境下运行任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @return 任务的封装结果
     */
    public UniversalTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        return s.runAsync(runnable);
    }

    /**
     * 在指定延迟后运行任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param delay 延迟时间（以服务器刻为单位）
     * @param entity 目标实体（可选）
     * @param location 目标位置（可选）
     * @return 任务的封装结果
     */
    public UniversalTask runTaskLater(final Plugin plugin, final Runnable runnable, long delay, Entity entity, Location location) {
        if (entity != null) {
            return s.runLater(entity, runnable, delay);
        } else if (location != null) {
            return s.runLater(location, runnable, delay);
        } else {
            return s.runLater(runnable, delay);
        }
    }

    public UniversalTask runTaskTimer(final Plugin plugin, final Runnable runnable, long delay, long period, Entity entity, Location location) {
        if (entity != null) {
            return s.runTimer(entity, runnable, delay, null, period);
        } else if (location != null) {
            return s.runTimer(location, runnable, delay, period);
        } else {
            return s.runTimer(runnable, delay, period);
        }
    }


    /**
     * 异步运行周期性任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param delay 初始延迟时间（以服务器刻为单位）
     * @param period 周期时间（以服务器刻为单位）
     * @return 任务的封装结果
     */
    public UniversalTask runTaskTimerAsynchronously(final Plugin plugin, final Runnable runnable, long delay, long period) {
        return s.runTimerAsync(runnable, delay, period);
    }

    /**
     * 异步运行延迟任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param delay 延迟时间（以服务器刻为单位）
     * @return 任务的封装结果
     */
    public UniversalTask runTaskLaterAsynchronously(final Plugin plugin, final Runnable runnable, long delay) {
        return s.runLaterAsync(runnable, delay);
    }

    /**
     * 在指定延迟后同步运行任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param delay 延迟时间（以服务器刻为单位）
     * @param entity 目标实体（可选）
     * @param location 目标位置（可选）
     * @return 任务的封装结果
     */
    public UniversalTask scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay, Entity entity, Location location) {
        if (entity != null) {
            return s.runLater(entity, runnable, delay);
        } else if (location != null) {
            return s.runLater(location, runnable, delay);
        } else {
            return s.runLater(runnable, delay);
        }
    }
}
