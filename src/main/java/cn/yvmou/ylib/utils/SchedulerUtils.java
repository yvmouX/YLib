package cn.yvmou.ylib.utils;

import cn.yvmou.ylib.YlibR;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * 专门用来使bukkit插件适配folia的懒狗工具
 * 提供任务调度工具，支持 Bukkit 和 Folia 的任务调度。
 */
public class SchedulerUtils {
    private final static UniversalScheduler s = YlibR.getScheduler();

    private SchedulerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * 检查当前服务器是否运行在 Folia 环境下。
     *
     * @return 如果服务器运行在 Folia 环境下返回 true，否则返回 false。
     */
    public static boolean isFolia() {
        return YlibR.isFolia();
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
    public static UniversalTask runTask(Plugin plugin, Runnable runnable, Entity entity, Location location) {
        if (entity != null) {
            return s.run(entity, runnable);
        } else if (location != null) {
            return s.run(location, runnable);
        } else {
            return s.run(runnable);
        }
    }

    /**
     * 在当前服务器环境下运行任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @return 任务的封装结果
     */
    public static UniversalTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
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
    public static UniversalTask runTaskLater(final Plugin plugin, final Runnable runnable, long delay, Entity entity, Location location) {
        if (entity != null) {
            return s.runLater(entity, runnable, delay);
        } else if (location != null) {
            return s.runLater(location, runnable, delay);
        } else {
            return s.runLater(runnable, delay);
        }
    }

    public static UniversalTask runTaskTimer(final Plugin plugin, final Runnable runnable, long delay, long period, Entity entity, Location location) {
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
    public static UniversalTask runTaskTimerAsynchronously(final Plugin plugin, final Runnable runnable, long delay, long period) {
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
    public static UniversalTask runTaskLaterAsynchronously(final Plugin plugin, final Runnable runnable, long delay) {
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
    public static UniversalTask scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay, Entity entity, Location location) {
        if (entity != null) {
            return s.runLater(entity, runnable, delay);
        } else if (location != null) {
            return s.runLater(location, runnable, delay);
        } else {
            return s.runLater(runnable, delay);
        }
    }
}

