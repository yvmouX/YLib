package cn.yvmou.ylib.impl.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.scheduler.UniversalTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class FoliaSchedulerImpl implements UniversalScheduler {
    protected final Plugin plugin;
    private final GlobalRegionScheduler globalRegionScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaSchedulerImpl(Plugin plugin, GlobalRegionScheduler globalRegionScheduler, RegionScheduler regionScheduler, AsyncScheduler asyncScheduler) {
        this.plugin = plugin;
        this.globalRegionScheduler = globalRegionScheduler;
        this.regionScheduler = regionScheduler;
        this.asyncScheduler = asyncScheduler;
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
        return new FoliaTaskImpl(globalRegionScheduler.run(plugin, task -> runnable.run()));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Runnable runnable) {
        return new FoliaTaskImpl(globalRegionScheduler.run(plugin, task -> runnable.run()));
    }

    @Override
    public UniversalTask runTask(Location location, Runnable runnable) {
        return new FoliaTaskImpl(regionScheduler.run(plugin, location, task -> runnable.run()));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Location location, Runnable runnable) {
        return new FoliaTaskImpl(regionScheduler.run(plugin, location, task -> runnable.run()));
    }

    @Override
    public UniversalTask runTask(Entity entity, Runnable runnable) {
        return new FoliaTaskImpl(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }

    @Override
    public UniversalTask runTask(Plugin plugin, Entity entity, Runnable runnable) {
        return new FoliaTaskImpl(entity.getScheduler().run(plugin, task -> runnable.run(), null));
    }

    @Override
    public UniversalTask runLater(Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(globalRegionScheduler.runDelayed(plugin, task -> runnable.run(), delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(globalRegionScheduler.runDelayed(plugin, task -> runnable.run(), delay));
    }

    @Override
    public UniversalTask runLater(Location location, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(regionScheduler.runDelayed(plugin, location, task -> runnable.run(), delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(regionScheduler.runDelayed(plugin, location, task -> runnable.run(), delay));
    }

    @Override
    public UniversalTask runLater(Entity entity, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delay));
    }

    @Override
    public UniversalTask runLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delay));
    }

    @Override
    public UniversalTask runTimer(Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(globalRegionScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(globalRegionScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay, period));
    }

    @Override
    public UniversalTask runTimer(Location location, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(regionScheduler.runAtFixedRate(plugin, location, task -> runnable.run(), delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(regionScheduler.runAtFixedRate(plugin, location, task -> runnable.run(), delay, period));
    }

    @Override
    public UniversalTask runTimer(Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(entity.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), retired, delay, period));
    }

    @Override
    public UniversalTask runTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, @org.jetbrains.annotations.Nullable Runnable retired, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(entity.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), retired, delay, period));
    }

    @Override
    public UniversalTask runAsync(Runnable runnable) {
        return new FoliaTaskImpl(asyncScheduler.runNow(plugin, task -> runnable.run()));
    }

    @Override
    public UniversalTask runAsync(Plugin plugin, Runnable runnable) {
        return new FoliaTaskImpl(asyncScheduler.runNow(plugin, task -> runnable.run()));
    }

    @Override
    public UniversalTask runLaterAsync(Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(asyncScheduler.runDelayed(plugin, task -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS));
    }

    @Override
    public UniversalTask runLaterAsync(Plugin plugin, Runnable runnable, long delay) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(asyncScheduler.runDelayed(plugin, task -> runnable.run(), delay * 50, TimeUnit.MILLISECONDS));
    }

    @Override
    public UniversalTask runTimerAsync(Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(asyncScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay * 50, period, TimeUnit.MILLISECONDS));
    }

    @Override
    public UniversalTask runTimerAsync(Plugin plugin, Runnable runnable, long delay, long period) {
        delay = checkDelay(delay);
        return new FoliaTaskImpl(asyncScheduler.runAtFixedRate(plugin, task -> runnable.run(), delay * 50, period, TimeUnit.MILLISECONDS));
    }

    @Override
    public void cancelAllTask() {
        globalRegionScheduler.cancelTasks(plugin);
        asyncScheduler.cancelTasks(plugin);
    }

    @Override
    public void cancelTask(UniversalTask task) {
        task.cancel();
    }

    @Override
    public void teleportAsync(Entity entity, Location location) {
        entity.teleportAsync(location);
    }

    private long checkDelay(long delay) {
        if (delay <= 0) {
            return 1;
        }
        return delay;
    }
}
