package cn.yvmou.ylib.folia.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Folia调度器管理器实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class FoliaScheduler implements UniversalScheduler {

    private final Plugin plugin;
    private final io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalRegionScheduler;
    private final io.papermc.paper.threadedregions.scheduler.RegionScheduler regionScheduler;
    private final io.papermc.paper.threadedregions.scheduler.AsyncScheduler asyncScheduler;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.globalRegionScheduler = Bukkit.getGlobalRegionScheduler();
        this.regionScheduler = Bukkit.getRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }

    @Override
    public boolean isFolia() {
        return true;
    }


    @Override
    public UniversalTask runTask(@NotNull Runnable runnable) {
        return new FoliaTask(globalRegionScheduler.run(plugin, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runTask(Plugin plugin, @NotNull Runnable runnable) {
        return new FoliaTask(globalRegionScheduler.run(plugin, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runTask(Location location, @NotNull Runnable runnable) {
        return new FoliaTask(regionScheduler.run(plugin, location, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runTask(Plugin plugin, Location location, @NotNull Runnable runnable) {
        return new FoliaTask(regionScheduler.run(plugin, location, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runTask(Entity entity, @NotNull Runnable runnable) {
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runTask(Plugin plugin, Entity entity, @NotNull Runnable runnable) {
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runLater(@NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(globalRegionScheduler.runDelayed(plugin, task -> runnable.run(), delay));
    }
    
    @Override
    public UniversalTask runLater(Plugin plugin, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(globalRegionScheduler.runDelayed(plugin, task -> runnable.run(), delay));
    }
    
    @Override
    public UniversalTask runLater(Location location, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(regionScheduler.runDelayed(plugin, location, task -> runnable.run(), delay));
    }
    
    @Override
    public UniversalTask runLater(Plugin plugin, Location location, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(regionScheduler.runDelayed(plugin, location, task -> runnable.run(), delay));
    }
    
    @Override
    public UniversalTask runLater(Entity entity, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runLater(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runTimer(@NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(globalRegionScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay, period));
    }
    
    @Override
    public UniversalTask runTimer(Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(globalRegionScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay, period));
    }
    
    @Override
    public UniversalTask runTimer(Location location, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(regionScheduler.runAtFixedRate(plugin, location, task -> runnable.run(), delay, period));
    }
    
    @Override
    public UniversalTask runTimer(Plugin plugin, Location location, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(regionScheduler.runAtFixedRate(plugin, location, task -> runnable.run(), delay, period));
    }
    
    @Override
    public UniversalTask runTimer(Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runTimer(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }
    
    @Override
    public UniversalTask runAsync(@NotNull Runnable runnable) {
        return new FoliaTask(asyncScheduler.runNow(plugin, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runAsync(Plugin plugin, @NotNull Runnable runnable) {
        return new FoliaTask(asyncScheduler.runNow(plugin, task -> runnable.run()));
    }
    
    @Override
    public UniversalTask runLaterAsync(@NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(asyncScheduler.runDelayed(plugin, task -> runnable.run(), delay * 50L, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    
    @Override
    public UniversalTask runLaterAsync(Plugin plugin, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTask(asyncScheduler.runDelayed(plugin, task -> runnable.run(), delay * 50L, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    
    @Override
    public UniversalTask runTimerAsync(@NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(asyncScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay * 50L, period * 50L, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    
    @Override
    public UniversalTask runTimerAsync(Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTask(asyncScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay * 50L, period * 50L, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    
    @Override
    public void cancelAllTasks(Plugin plugin) {
        globalRegionScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }
    
    @Override
    public void cancelTask(UniversalTask universalTask) {
        if (universalTask instanceof FoliaTask) {
            universalTask.cancel();
        }
    }
    
    @Override
    public void teleportAsync(Entity entity, Location location) {
        asyncScheduler.runNow(plugin, task -> entity.teleport(location));
    }

    private long checkDelay(long delay) {
        if (delay <= 0) {
            return 1;
        }
        return delay;
    }
}