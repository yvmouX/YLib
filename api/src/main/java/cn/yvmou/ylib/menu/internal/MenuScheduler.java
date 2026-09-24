package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * 「下一 tick 再跑」这件事的唯一实现点。
 * <p>
 * 需要它的地方只有一处：`delayedRefresh(true)` 要把重画推迟一 tick。三级回退的顺序与
 * 其它模块一致——**平台的统一调度器优先**（Folia / Canvas 上必须用它，直接碰 Bukkit 调度器会抛），
 * 其次 Bukkit，最后就地执行。
 * <p>
 * 最后那一级是刻意的：拿不到调度器（单测、关服中）时**就地跑**比静默丢掉好——
 * 刷新界面的意图不该因为「没有服务端」而消失。
 */
public final class MenuScheduler {

    private MenuScheduler() {
    }

    /**
     * 在下一 tick 执行 {@code task}。
     *
     * @param plugin 宿主插件；为 {@code null} 时只能用平台调度器或就地执行
     */
    public static void runNextTick(Plugin plugin, Runnable task) {
        if (task == null) {
            return;
        }
        UniversalScheduler scheduler = scheduler(plugin);
        if (scheduler != null) {
            try {
                scheduler.runLater(task, 1L);
                return;
            } catch (RuntimeException unavailable) {
                // 落到下一级
            }
        }
        if (plugin != null) {
            try {
                Bukkit.getScheduler().runTask(plugin, task);
                return;
            } catch (RuntimeException noServer) {
                // 关服中：落到就地执行，别把刷新吞掉
            }
        }
        task.run();
    }

    /** 平台的统一调度器；拿不到（未初始化 / 平台模块缺失）返回 {@code null}。 */
    private static UniversalScheduler scheduler(Plugin plugin) {
        try {
            cn.yvmou.ylib.YLib ylib = cn.yvmou.ylib.YLib.instance();
            return ylib == null ? null : ylib.getScheduler();
        } catch (RuntimeException unavailable) {
            return null;
        }
    }
}
