package cn.yvmou.ylib.folia.scheduler;

import cn.yvmou.ylib.api.scheduler.SchedulerManager;
import cn.yvmou.ylib.api.scheduler.Task;
import cn.yvmou.ylib.folia.task.FoliaTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Folia调度器管理器实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class FoliaSchedulerManager implements SchedulerManager {
    
    private final Plugin plugin;
    private final BukkitScheduler globalScheduler;
    private final io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler regionScheduler;
    private final io.papermc.paper.threadedregions.scheduler.RegionScheduler entityScheduler;
    private final io.papermc.paper.threadedregions.scheduler.AsyncScheduler asyncScheduler;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public FoliaSchedulerManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.globalScheduler = Bukkit.getScheduler();
        this.regionScheduler = Bukkit.getGlobalRegionScheduler();
        this.entityScheduler = Bukkit.getRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }
    
    @Override
    public boolean isFolia() {
        return true;
    }
    
    @Override
    public Task runTask(@NotNull Runnable runnable) {
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Location location, @NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Entity entity, @NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runTask(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTask(plugin, runnable));
    }
    
    @Override
    public Task runLater(@NotNull Runnable runnable, long delay) {
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Location location, @NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLater(plugin, runnable, delay));
    }
    
    @Override
    public Task runTimer(@NotNull Runnable runnable, long delay, long period) {
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable runnable, long delay, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimer(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimer(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runAsync(@NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskAsynchronously(plugin, runnable));
    }
    
    @Override
    public Task runAsync(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskAsynchronously(plugin, runnable));
    }
    
    @Override
    public Task runLaterAsync(@NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }
    
    @Override
    public Task runLaterAsync(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }
    
    @Override
    public Task runTimerAsync(@NotNull Runnable runnable, long delay, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }
    
    @Override
    public Task runTimerAsync(@NotNull Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        // 使用全局调度器作为后备方案
        return new FoliaTask(globalScheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }
    
    @Override
    public void cancelAllTasks() {
        globalScheduler.cancelTasks(plugin);
        // Folia的调度器没有cancel方法，使用全局调度器作为后备方案
    }
    
    @Override
    public void cancelTask(@NotNull Task task) {
        if (task instanceof FoliaTask) {
            ((FoliaTask) task).cancel();
        }
    }
    
    @Override
    public void teleportAsync(@NotNull Entity entity, @NotNull Location location) {
        asyncScheduler.runNow(plugin, task -> entity.teleport(location));
    }
} 