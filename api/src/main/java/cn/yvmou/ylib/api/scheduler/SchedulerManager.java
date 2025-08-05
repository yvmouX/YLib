package cn.yvmou.ylib.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

/**
 * 调度器管理器接口 - 提供统一的调度器管理功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public interface SchedulerManager {
    
    /**
     * 检查是否为Folia服务器
     * @return boolean 如果是Folia服务器返回true
     */
    boolean isFolia();
    
    /**
     * 运行同步任务
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Runnable runnable);
    
    /**
     * 运行同步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Plugin plugin, Runnable runnable);
    
    /**
     * 运行同步任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Location location, Runnable runnable);
    
    /**
     * 运行同步任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Plugin plugin, Location location, Runnable runnable);
    
    /**
     * 运行同步任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Entity entity, Runnable runnable);
    
    /**
     * 运行同步任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runTask(Plugin plugin, Entity entity, Runnable runnable);
    
    /**
     * 延迟运行同步任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Plugin plugin, Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Location location, Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Plugin plugin, Location location, Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Entity entity, Runnable runnable, long delay);
    
    /**
     * 延迟运行同步任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLater(Plugin plugin, Entity entity, Runnable runnable, long delay);
    
    /**
     * 运行定时任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Plugin plugin, Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于位置）
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Location location, Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于位置）
     * @param plugin 插件实例
     * @param location 位置
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period);
    
    /**
     * 运行定时任务（基于实体）
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param retired 实体死亡时的回调
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period);
    
    /**
     * 运行定时任务（基于实体）
     * @param plugin 插件实例
     * @param entity 实体
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param retired 实体死亡时的回调
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period);
    
    /**
     * 运行异步任务
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runAsync(Runnable runnable);
    
    /**
     * 运行异步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @return Task 任务实例
     */
    Task runAsync(Plugin plugin, Runnable runnable);
    
    /**
     * 延迟运行异步任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLaterAsync(Runnable runnable, long delay);
    
    /**
     * 延迟运行异步任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @return Task 任务实例
     */
    Task runLaterAsync(Plugin plugin, Runnable runnable, long delay);
    
    /**
     * 运行异步定时任务
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimerAsync(Runnable runnable, long delay, long period);
    
    /**
     * 运行异步定时任务
     * @param plugin 插件实例
     * @param runnable 要执行的任务
     * @param delay 延迟时间（tick）
     * @param period 周期时间（tick）
     * @return Task 任务实例
     */
    Task runTimerAsync(Plugin plugin, Runnable runnable, long delay, long period);
    
    /**
     * 取消所有任务
     */
    void cancelAllTasks();
    
    /**
     * 取消指定任务
     * @param task 要取消的任务
     */
    void cancelTask(Task task);
    
    /**
     * 异步传送实体
     * @param entity 实体
     * @param location 目标位置
     */
    void teleportAsync(Entity entity, Location location);
} 