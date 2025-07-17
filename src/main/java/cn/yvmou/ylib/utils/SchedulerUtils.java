package cn.yvmou.ylib.utils;

import cn.yvmou.ylib.JavaPluginR.YLibHolder;
import com.tcoded.folialib.enums.EntityTaskResult;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

/**
 * 提供任务调度工具，支持 Bukkit 和 Folia 的任务调度。
 */
public class SchedulerUtils {
    /**
     * 私有构造函数，防止实例化。
     */
    private SchedulerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * 检查当前服务器是否运行在 Folia 环境下。
     *
     * @return 如果服务器运行在 Folia 环境下返回 true，否则返回 false。
     */
    public static boolean isFolia() {
        // 这里假设YLib实例已通过插件生命周期管理好
        return YLibHolder.getYLib().getFoliaLib().isFolia();
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
        if (isFolia()) {
            if (entity != null) {
                CompletableFuture<EntityTaskResult> task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtEntity(entity, wrappedTask -> runnable.run());
                return new UniversalTask(null, null, task, -1);
            } else if (location != null) {
                CompletableFuture<Void> task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtLocation(location, wrappedTask -> runnable.run());
                return new UniversalTask(null, null, task, -1);
            } else {
                CompletableFuture<Void> task = YLibHolder.getYLib().getFoliaLib().getScheduler().runNextTick(wrappedTask -> runnable.run());
                return new UniversalTask(null, null, task, -1);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return new UniversalTask(null, null, null, -1);
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
        if (isFolia()) {
            CompletableFuture<Void> task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAsync(wrappedTask -> runnable.run());
            return new UniversalTask(null, null, task, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new UniversalTask(task, null, null, -1);
        }
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
        if (isFolia()) {
            WrappedTask task;
            if (entity != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtEntityLater(entity, runnable, delay);
            } else if (location != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtLocationLater(location, runnable, delay);
            } else {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runLater(runnable, delay);
            }
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            return new UniversalTask(task, null, null, -1);
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
        if (isFolia()) {
            WrappedTask task = YLibHolder.getYLib().getFoliaLib().getScheduler().runTimerAsync(runnable, delay, period);
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
            return new UniversalTask(task, null, null, -1);
        }
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
        if (isFolia()) {
            WrappedTask task = YLibHolder.getYLib().getFoliaLib().getScheduler().runLaterAsync(runnable, delay);
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new UniversalTask(task, null, null, -1);
        }
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
        if (isFolia()) {
            WrappedTask task;
            if (entity != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtEntityLater(entity, runnable, delay);
            } else if (location != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtLocationLater(location, runnable, delay);
            } else {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runLater(runnable, delay);
            }
            return new UniversalTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
            return new UniversalTask(null, null, null, taskId);
        }
    }

    /**
     * 同步运行周期性任务。
     *
     * @param plugin 插件实例
     * @param runnable 要运行的任务
     * @param delay 初始延迟时间（以服务器刻为单位）
     * @param period 周期时间（以服务器刻为单位）
     * @param entity 目标实体（可选）
     * @param location 目标位置（可选）
     * @return 任务的封装结果
     */
    public static UniversalTask scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long period, Entity entity, Location location) {
        if (isFolia()) {
            WrappedTask task;
            if (entity != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtEntityTimer(entity, runnable, delay, period);
            } else if (location != null) {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runAtLocationTimer(location, runnable, delay, period);
            } else {
                task = YLibHolder.getYLib().getFoliaLib().getScheduler().runTimer(runnable, delay, period);
            }
            return new UniversalTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
            return new UniversalTask(null, null, null, taskId);
        }
    }

    /**
     * 统一封装任务结果。
     *
     * @param bukkitTask Bukkit 任务实例（在非 Folia 环境下使用）
     * @param foliaTask Folia 任务实例（在 Folia 环境下使用）
     * @param future 任务的异步结果（如果适用）
     * @param taskId 任务的 ID（在 Bukkit 环境下使用）
     */
    public record UniversalTask(BukkitTask bukkitTask, WrappedTask foliaTask, CompletableFuture<?> future, int taskId) {

        /**
         * 取消任务
         */
        public void cancel() {
            if (SchedulerUtils.isFolia()) {
                if (foliaTask != null) foliaTask.cancel();
                if (future != null) future.cancel(true);
            } else {
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                } else if (taskId != -1) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }

        /**
         * 检查任务是否已取消。
         *
         * @return 如果任务已取消返回 true，否则返回 false。
         */
        public boolean isCancelled() {
            if (SchedulerUtils.isFolia()) {
                return foliaTask != null && foliaTask.isCancelled();
            } else {
                if (bukkitTask != null) {
                    return bukkitTask.isCancelled();
                }
                return !Bukkit.getScheduler().isCurrentlyRunning(taskId) && !Bukkit.getScheduler().isQueued(taskId);
            }
        }
    }
}

