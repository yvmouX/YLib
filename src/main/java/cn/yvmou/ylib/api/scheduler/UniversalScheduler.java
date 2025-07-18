package cn.yvmou.ylib.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;

/**
 * 通用调度程序
 *
 * @author admin
 * &#064;date  2025/07/19
 */
public interface UniversalScheduler {
    /**
     * 跑
     *
     * @param runnable 可运行
     * @return {@link UniversalTask }
     */
    UniversalTask run(Runnable runnable);

    /**
     * 跑
     *
     * @param location 位置
     * @param runnable 可运行
     * @return {@link UniversalTask }
     */
    UniversalTask run(Location location, Runnable runnable);

    /**
     * 跑
     *
     * @param entity   实体
     * @param runnable 可运行
     * @return {@link UniversalTask }
     */
    UniversalTask run(Entity entity, Runnable runnable);

    /**
     * 稍后运行
     *
     * @param runnable 可运行
     * @param delay    延迟
     * @return {@link UniversalTask }
     */
    UniversalTask runLater(Runnable runnable, long delay);

    /**
     * 稍后运行
     *
     * @param location 位置
     * @param runnable 可运行
     * @param delay    延迟
     * @return {@link UniversalTask }
     */
    UniversalTask runLater(Location location, Runnable runnable, long delay);

    /**
     * 稍后运行
     *
     * @param entity   实体
     * @param runnable 可运行
     * @param delay    延迟
     * @return {@link UniversalTask }
     */
    UniversalTask runLater(Entity entity, Runnable runnable, long delay);

    /**
     * 运行计时器
     *
     * @param runnable 可运行
     * @param delay    延迟
     * @param period   时期
     * @return {@link UniversalTask }
     */
    UniversalTask runTimer(Runnable runnable, long delay, long period);

    /**
     * 运行计时器
     *
     * @param location 位置
     * @param runnable 可运行
     * @param delay    延迟
     * @param period   时期
     * @return {@link UniversalTask }
     */
    UniversalTask runTimer(Location location, Runnable runnable, long delay, long period);

    /**
     * 运行计时器
     *
     * @param entity   实体
     * @param runnable 可运行
     * @param delay    延迟
     * @param retired  退休
     * @param period   时期
     * @return {@link UniversalTask }
     */
    UniversalTask runTimer(Entity entity, Runnable runnable, long delay, @Nullable Runnable retired, long period);

    /**
     * 异步运行
     *
     * @param runnable 可运行
     * @return {@link UniversalTask }
     */
    UniversalTask runAsync(Runnable runnable);

    /**
     * 稍后异步运行
     *
     * @param runnable 可运行
     * @param delay    延迟
     * @return {@link UniversalTask }
     */
    UniversalTask runLaterAsync(Runnable runnable, long delay);

    /**
     * 运行计时器异步
     *
     * @param runnable 可运行
     * @param delay    延迟
     * @param period   时期
     * @return {@link UniversalTask }
     */
    UniversalTask runTimerAsync(Runnable runnable, long delay, long period);

    /**
     * 取消所有任务
     */
    void cancelAllTask();

    /**
     * 取消任务
     *
     * @param task 任务
     */
    void cancelTask(UniversalTask task);

    /**
     * 异步传送
     *
     * @param entity   实体
     * @param location 位置
     */
    void teleportAsync(Entity entity, Location location);
}
