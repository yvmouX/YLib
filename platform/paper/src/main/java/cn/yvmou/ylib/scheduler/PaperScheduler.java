package cn.yvmou.ylib.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper调度器管理器实现
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class PaperScheduler implements UniversalScheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    @Override
    public boolean isFolia() {
        return false;
    }

    @Override
    public UniversalTask runTask(@NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Location location, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Location location, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Entity entity, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Entity entity, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runLater(@NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Location location, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Location location, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Entity entity, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runTimer(@NotNull Runnable runnable, long delay, long period) {
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Location location, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Location location, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Entity entity, @NotNull Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runAsync(@NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public UniversalTask runAsync(Plugin plugin, @NotNull Runnable runnable) {
        return new PaperTask(scheduler.runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public UniversalTask runLaterAsync(@NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLaterAsync(Plugin plugin, @NotNull Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runTimerAsync(@NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimerAsync(Plugin plugin, @NotNull Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new PaperTask(scheduler.runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    @Override
    public void cancelAllTasks(Plugin plugin) {
        scheduler.cancelTasks(plugin);
    }

    @Override
    public void cancelTask(UniversalTask universalTask) {
        if (universalTask instanceof PaperTask) {
            universalTask.cancel();
        }
    }

    @Override
    public void teleportAsync(Entity entity, Location location) {
        scheduler.runTaskAsynchronously(plugin, () -> entity.teleport(location));
    }

    private long checkDelay(long delay) {
        if (delay <= 0) {
            return 1;
        }
        return delay;
    }
}