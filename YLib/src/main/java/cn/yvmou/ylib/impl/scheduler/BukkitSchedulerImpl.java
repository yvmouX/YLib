package cn.yvmou.ylib.impl.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class BukkitSchedulerImpl implements UniversalScheduler {
    private final Plugin plugin;

    public BukkitSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isFolia() {
        boolean isFolia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {
        }
        return isFolia;
    }

    @Override
    public UniversalTask runTask(Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Location location, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Location location, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Entity entity, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Entity entity, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTask(plugin, runnable));
    }

    @Override
    public UniversalTask runLater(Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Location location, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Entity entity, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLater(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runTimer(Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Location location, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runAsync(Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public UniversalTask runAsync(Plugin plugin, Runnable runnable) {
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public UniversalTask runLaterAsync(Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runLaterAsync(Plugin plugin, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay));
    }

    @Override
    public UniversalTask runTimerAsync(Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    @Override
    public UniversalTask runTimerAsync(Plugin plugin, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new BukkitTaskImpl(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period));
    }

    @Override
    public void cancelAllTask() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public void cancelTask(UniversalTask task) {
        task.cancel();
    }

    @Override
    public void teleportAsync(Entity entity, Location location) {
        entity.teleport(location);
    }

    private long checkDelay(long delay) {
        if (delay <= 0) {
            return 1;
        }
        return delay;
    }
}
