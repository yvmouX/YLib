//package cn.yvmou.ylib.scheduler;
//
//import org.bukkit.Location;
//import org.bukkit.entity.Entity;
//import org.bukkit.plugin.Plugin;
//import org.jetbrains.annotations.NotNull;
//
//public class UniversalRunnable implements Runnable {
//    private UniversalTask task;
//
//    /**
//     * Returns true if this task has been cancelled.
//     *
//     * @return true if the task has been cancelled
//     * @throws IllegalStateException if task was not scheduled yet
//     */
//    public synchronized boolean isCancelled() throws IllegalStateException {
//        checkScheduled();
//        return task.isCancelled();
//    }
//
//    /**
//     * Attempts to cancel this task.
//     *
//     * @throws IllegalStateException if task was not scheduled yet
//     */
//    public synchronized void cancel() throws IllegalStateException {
//        getTask().cancel();
//    }
//
//    /**
//     * Schedules this in the Bukkit scheduler to run on next tick.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runTask(Runnable)
//     */
//    @NotNull
//    public synchronized UniversalTask runTask(@NotNull Plugin plugin) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(plugin, this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTask(@NotNull Plugin plugin, @NotNull Location location) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(plugin, location, this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTask(@NotNull Plugin plugin, @NotNull Entity entity) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(plugin, entity, this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTask() throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTask(@NotNull Location location) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(location, this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTask(@NotNull Entity entity) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTask(entity, this));
//    }
//
//    /**
//     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
//     * should be taken to assure the thread-safety of asynchronous tasks.</b>
//     * <p>
//     * Schedules this in the Bukkit scheduler to run asynchronously.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runAsync(Runnable)
//     */
//    @NotNull
//    public synchronized UniversalTask runTaskAsynchronously(@NotNull Plugin plugin) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runAsync(plugin, this));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTaskAsynchronously() throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runAsync(this));
//    }
//
//    /**
//     * Schedules this to run after the specified number of server ticks.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @param delay  the ticks to wait before running the task
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runLater(Runnable, long)
//     */
//    @NotNull
//    public synchronized UniversalTask runLater(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(plugin, this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLater(@NotNull Plugin plugin, Location location, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(plugin, location, this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLater(@NotNull Plugin plugin, Entity entity, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(plugin, entity, this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLater(long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLater(Location location, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(location, this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLater(Entity entity, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLater(entity, this, delay));
//    }
//
//    /**
//     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
//     * should be taken to assure the thread-safety of asynchronous tasks.</b>
//     * <p>
//     * Schedules this to run asynchronously after the specified number of
//     * server ticks.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @param delay  the ticks to wait before running the task
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runLaterAsync(Runnable, long)
//     */
//    @NotNull
//    public synchronized UniversalTask runLaterAsync(@NotNull Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLaterAsync(plugin, this, delay));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runLaterAsync(long delay) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runLaterAsync(this, delay));
//    }
//
//    /**
//     * Schedules this to repeatedly run until cancelled, starting after the
//     * specified number of server ticks.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @param delay  the ticks to wait before running the task
//     * @param period the ticks to wait between runs
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runTimer(Runnable, long, long)
//     */
//    @NotNull
//    public synchronized UniversalTask runTimer(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(plugin, this, delay, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimer(@NotNull Plugin plugin, Location location, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(plugin, location, this, delay, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimer(@NotNull Plugin plugin, Entity entity, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(plugin, entity, this, delay, null, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimer(long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(this, delay, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimer(Location location, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(location, this, delay, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimer(Entity entity, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimer(entity, this, delay, null, period));
//    }
//
//    /**
//     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
//     * should be taken to assure the thread-safety of asynchronous tasks.</b>
//     * <p>
//     * Schedules this to repeatedly run asynchronously until cancelled,
//     * starting after the specified number of server ticks.
//     *
//     * @param plugin the reference to the plugin scheduling task
//     * @param delay  the ticks to wait before running the task for the first
//     *               time
//     * @param period the ticks to wait between runs
//     * @return a BukkitTask that contains the id number
//     * @throws IllegalArgumentException if plugin is null
//     * @throws IllegalStateException    if this was already scheduled
//     * @see UniversalScheduler#runTimerAsync(Runnable, long, long)
//     */
//    @NotNull
//    public synchronized UniversalTask runTimerAsync(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimerAsync(plugin, this, delay, period));
//    }
//
//    @NotNull
//    public synchronized UniversalTask runTimerAsync(long delay, long period) throws IllegalArgumentException, IllegalStateException {
//        checkNotYetScheduled();
//        return setupTask(YLib.getyLib().getScheduler().runTimerAsync(this, delay, period));
//    }
//
//    /**
//     * Gets the task id for this runnable.
//     *
//     * @return the task id that this runnable was scheduled as
//     * @throws IllegalStateException if task was not scheduled yet
//     */
//    public synchronized UniversalTask getTask() throws IllegalStateException {
//        checkScheduled();
//        return task;
//    }
//
//    private void checkScheduled() {
//        if (task == null) {
//            throw new IllegalStateException("Not scheduled yet");
//        }
//    }
//
//    private void checkNotYetScheduled() {
//        if (task != null) {
//            throw new IllegalStateException("Already scheduled as " + task);
//        }
//    }
//
//    @NotNull
//    private UniversalTask setupTask(@NotNull final UniversalTask task) {
//        this.task = task;
//        return task;
//    }
//}
