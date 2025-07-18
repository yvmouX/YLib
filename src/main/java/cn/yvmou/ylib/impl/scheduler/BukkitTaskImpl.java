package cn.yvmou.ylib.impl.scheduler;

import cn.yvmou.ylib.api.scheduler.UniversalTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitTaskImpl implements UniversalTask {
    private final org.bukkit.scheduler.BukkitTask task;

    public BukkitTaskImpl(org.bukkit.scheduler.BukkitTask task) {
        this.task = task;
    }

    @Override
    public Plugin getOwningPlugin() {
        return this.task.getOwner();
    }

    @Override
    public boolean isCancelled() {
        return this.task.isCancelled();
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }

    @Override
    public boolean isRepeatingTask() {
        return Bukkit.getServer().getScheduler().isQueued(this.task.getTaskId()) &&
                Bukkit.getServer().getScheduler().isCurrentlyRunning(this.task.getTaskId());
    }

    @Override
    public boolean isCurrentlyRunning() {
        return Bukkit.getServer().getScheduler().isCurrentlyRunning(this.task.getTaskId());
    }
}
