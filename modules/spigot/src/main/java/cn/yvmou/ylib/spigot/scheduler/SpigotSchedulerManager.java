package cn.yvmou.ylib.spigot.scheduler;

import cn.yvmou.ylib.api.scheduler.SchedulerManager;
import cn.yvmou.ylib.api.scheduler.Task;
import cn.yvmou.ylib.spigot.task.SpigotTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Spigot调度器管理器实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class SpigotSchedulerManager implements SchedulerManager {
    
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public SpigotSchedulerManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }
    
    @Override
    public boolean isFolia() {
        return false;
    }
    
    @Override
    public Task runTask(@NotNull Runnable runnable) {
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Location location, @NotNull Runnable runnable) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Entity entity, @NotNull Runnable runnable) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runLater(@NotNull Runnable runnable, long delay) {
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runTimer(@NotNull Runnable runnable, long delay, long period) {
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        // Spigot不支持基于位置的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        // Spigot不支持基于实体的调度，使用全局调度器
        return new SpigotTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runAsync(@NotNull Runnable runnable) {
        return new SpigotTask(scheduler.runTaskAsynchronously(plugin, runnable));
    }
    
    @Override
    public Task runAsync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        return new SpigotTask(scheduler.runTaskAsynchronously(plugin, runnable));
    }
    
    @Override
    public Task runLaterAsync(@NotNull Runnable runnable, long delay) {
        return new SpigotTask(scheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }
    
    @Override
    public Task runLaterAsync(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        return new SpigotTask(scheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }
    
    @Override
    public Task runTimerAsync(@NotNull Runnable runnable, long delay, long period) {
        return new SpigotTask(scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimerAsync(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        return new SpigotTask(scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }
    
    @Override
    public void cancelAllTasks() {
        scheduler.cancelTasks(plugin);
    }
    
    @Override
    public void cancelTask(@NotNull Task task) {
        if (task instanceof SpigotTask) {
            ((SpigotTask) task).cancel();
        }
    }
    
    @Override
    public void teleportAsync(@NotNull Entity entity, @NotNull Location location) {
        scheduler.runTaskAsynchronously(plugin, () -> entity.teleport(location));
    }
} 