package cn.yvmou.ylib.menu.internal;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * 「下一 tick 再跑」这件事的唯一实现点。
 *
 * <h2>它同时是菜单包的解耦点</h2>
 * 菜单只有一处需要调度器：{@code delayedRefresh(true)} 想把重画推迟一 tick，避免在玩家
 * 正拖着一叠物品时打断他。为此让整个菜单包依赖调度器模块并不划算。
 * <p>
 * 因此这里只认一个**函数式钩子** {@code Consumer<Runnable>}，不 import 平台调度器的任何类型：
 * 宿主在装配时把「下一 tick 执行」这件事注入进来即可（{@link Menus#scheduler}）。
 *
 * <h2>默认回退</h2>
 * 宿主不注入时走三级回退：**Bukkit 调度器 → 就地执行**。
 * <p>
 * 之所以默认不直接取平台的统一调度器：那会把 {@code YLib} 与 {@code scheduler} 包拽进菜单包的
 * 依赖里，而这正是本类要解掉的耦合。**Folia / Canvas 上请注入平台调度器** ——
 * 那两个平台上 Bukkit 调度器会抛，会一路落到「就地执行」（同步执行一次刷新，通常仍能工作，
 * 但不再是下一 tick）。注入方式见 {@link Menus#scheduler}。
 * <p>
 * 最后那一级是刻意的：拿不到调度器（单测、关服中）时**就地跑**比静默丢掉好——
 * 刷新界面的意图不该因为「没有服务端」而消失。
 */
public final class MenuScheduler {

    /** 宿主注入的「下一 tick 执行」实现；没注入时为 {@code null}。 */
    private static volatile Consumer<Runnable> sink;

    private MenuScheduler() {
    }

    /**
     * 注入「下一 tick 执行」的实现；传 {@code null} 恢复默认回退。
     * <p>
     * 由 {@link Menus#scheduler} 转发，宿主在 {@code onEnable} 里调一次即可。
     */
    public static void register(Consumer<Runnable> nextTick) {
        sink = nextTick;
    }

    /** 当前是否已注入（给自测与诊断用）。 */
    public static boolean injected() {
        return sink != null;
    }

    /**
     * 在下一 tick 执行 {@code task}。
     *
     * @param plugin 宿主插件；为 {@code null} 时只能用注入的实现或就地执行
     */
    public static void runNextTick(Plugin plugin, Runnable task) {
        if (task == null) {
            return;
        }
        Consumer<Runnable> injected = sink;
        if (injected != null) {
            try {
                injected.accept(task);
                return;
            } catch (RuntimeException failed) {
                // 宿主自己的实现抛了：落到默认回退，而不是把这次刷新丢掉
            }
        }
        if (plugin != null) {
            try {
                Bukkit.getScheduler().runTask(plugin, task);
                return;
            } catch (RuntimeException noServer) {
                // 关服中 / 平台不支持 Bukkit 调度器：落到就地执行，别把刷新吞掉
            }
        }
        task.run();
    }
}
