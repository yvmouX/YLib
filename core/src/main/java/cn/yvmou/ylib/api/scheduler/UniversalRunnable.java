package cn.yvmou.ylib.api.scheduler;

import cn.yvmou.ylib.YLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class UniversalRunnable implements Runnable {
    private UniversalTask task;

    public synchronized boolean isCancelled() throws IllegalStateException {
        checkScheduled();
        return task.isCancelled();
    }

    public synchronized void cancel() throws IllegalStateException {
        task.cancel();
    }

    @NotNull
    public synchronized UniversalTask runTask(@NotNull Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTask(plugin, this));
    }
    @NotNull
    public synchronized UniversalTask runTask() throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTask(this));
    }

    @NotNull
    public synchronized UniversalTask runAsync(@NotNull Plugin plugin) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runAsync(plugin, this));
    }
    @NotNull
    public synchronized UniversalTask runAsync() throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runAsync(this));
    }

    @NotNull
    public synchronized UniversalTask runLater(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runLater(plugin, this, delay));
    }
    @NotNull
    public synchronized UniversalTask runLater(long delay) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runLater(this, delay));
    }

    @NotNull
    public synchronized UniversalTask runLaterAsync(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runLaterAsync(plugin, this, delay));
    }
    @NotNull
    public synchronized UniversalTask runLaterAsync(long delay) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runLaterAsync(this, delay));
    }

    @NotNull
    public synchronized UniversalTask runTimer(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTimer(plugin, this, delay, period));
    }
    @NotNull
    public synchronized UniversalTask runTimer(long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTimer(this, delay, period));
    }

    @NotNull
    public synchronized UniversalTask runTimerAsync(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTimerAsync(plugin, this, delay, period));
    }
    @NotNull
    public synchronized  UniversalTask runTimerAsync(long delay, long period) throws IllegalArgumentException, IllegalStateException  {
        checkNotYetScheduled();
        return setupTask(YLib.instance.getScheduler().runTimerAsync(this, delay, period));
    }

    @Override
    public void run() {

    }

//    public synchronized int getTask() throws IllegalStateException {
//        checkScheduled();
//        return task.getTaskId();
//    }

    private void checkScheduled() {
        if (task == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
    }

    private void checkNotYetScheduled() {
        if (task != null) {
            throw new IllegalStateException("Already scheduled");
        }
    }

    @NotNull
    private UniversalTask setupTask(@NotNull final UniversalTask task) {
        this.task = task;
        return task;
    }
}
