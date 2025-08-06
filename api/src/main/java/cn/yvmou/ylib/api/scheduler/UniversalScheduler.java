package cn.yvmou.ylib.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * 调度器管理器接口 - 提供统一的调度器管理功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public interface UniversalScheduler {
    
    /**
     * 检查是否为Folia服务器
     * @return boolean 如果是Folia服务器返回true
     */
    boolean isFolia();
    
    /**
     * 运行同步任务
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(@NotNull Runnable runnable);
    
    /**
     * 运行同步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(Plugin plugin, @NotNull Runnable runnable);
    
    /**
     * 运行同步任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(Location location, @NotNull Runnable runnable);
    
    /**
     * 运行同步任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(Plugin plugin, Location location, @NotNull Runnable runnable);
    
    /**
     * 运行同步任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(Entity entity, @NotNull Runnable runnable);
    
    /**
     * 运行同步任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runTask(Plugin plugin, Entity entity, @NotNull Runnable runnable);
    
    /**
     * 延迟运行同步任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(@NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(Plugin plugin, @NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(Location location, @NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(Plugin plugin, Location location, @NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(Entity entity, @NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLater(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay);
    
    /**
     * 运行定时任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(@NotNull Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(Plugin plugin, @NotNull Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(Location location, @NotNull Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(Plugin plugin, Location location, @NotNull Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param retired 实体死亡时的回调
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period);
    
    /**
     * 运行定时任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param retired 实体死亡时的回调
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimer(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period);
    
    /**
     * 运行异步任务
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runAsync(@NotNull Runnable runnable);
    
    /**
     * 运行异步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @return UniversalTask 任务实例
     */
    UniversalTask runAsync(Plugin plugin, @NotNull Runnable runnable);
    
    /**
     * 延迟运行异步任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLaterAsync(@NotNull Runnable runnable, long delay);
    
    /**
     * 延迟运行异步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runLaterAsync(Plugin plugin, @NotNull Runnable runnable, long delay);
    
    /**
     * 运行异步定时任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimerAsync(@NotNull Runnable runnable, long delay, long period);
    
    /**
     * 运行异步定时任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return UniversalTask 任务实例
     */
    UniversalTask runTimerAsync(Plugin plugin, @NotNull Runnable runnable, long delay, long period);
    
    /**
     * 取消所有任务
     */
    void cancelAllTasks(Plugin plugin);
    
    /**
     * 取消指定任务
     * @param universalTask 要取消的任务
     */
    void cancelTask(UniversalTask universalTask);
    
    /**
     * 异步传送实体
     * @param entity 实体
     * @param location 目标位置
     */
    void teleportAsync(Entity entity, Location location);
} 