package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * 聊天栏取值：一问一答，值回传到主线程。
 * <p>
 * 输入「取消」/cancel、超时、退服都算放弃。拦截聊天由 {@link InputListener} 完成，
 * 宿主只调一次 {@link MenuListener#init(Plugin)} 两样都注册好。
 */
public final class PlayerInput {

    /** 超时秒数。 */
    public static final int TIMEOUT_SECONDS = 90;

    private PlayerInput() {
    }

    /** 让玩家在聊天栏输入一个值：提示 → 回车提交；输入「取消」/cancel、超时、退服都会放弃。 */
    public static void ask(Player player, String title, String hint, String current,
                           Consumer<String> onValue, final Runnable onCancel) {
        if (player == null) {
            return;
        }
        // 容器界面开着时聊天键被界面吃掉，玩家根本没法打字：先关掉它（完成/取消后由调用方重新打开菜单）
        try {
            player.closeInventory();
        } catch (Throwable noServer) {
            // 关服中或没有服务端（单测）：关不掉也不该打断这次输入
        }
        // 旧状态先丢：玩家中途点了别的字段时，旧回调不能因为这次输入被触发
        forget(player);
        final Inputs.Pending pending = Inputs.of(player.getUniqueId(), title, new Inputs.Callback() {
            @Override
            public void value(String text) {
                if (onValue != null) {
                    onValue.accept(text);
                }
            }

            @Override
            public void cancel() {
                if (onCancel != null) {
                    onCancel.run();
                }
            }
        });
        Inputs.put(pending);
        prompt(player, title, hint, current);
        UniversalScheduler scheduler = scheduler();
        if (scheduler == null) {
            // 拿不到调度器就没有超时兜底：宁可让这次输入一直等到玩家自己说话，也不能抛
            return;
        }
        // 没人可能在 90 秒里输不完；不设超时的话玩家的正常聊天会被无限期吞掉
        pending.setTimeout(scheduler.runLater(player, new Runnable() {
            @Override
            public void run() {
                // 身份比较而不是按 UUID 摘：玩家中途重开输入时不该被旧任务踢掉
                if (!Inputs.take(pending)) {
                    return;
                }
                if (player.isOnline()) {
                    player.sendMessage(TextRenderer.render("&c输入超时，已放弃「" + pending.label() + "」"));
                }
                pending.callback().cancel();
            }
        }, TIMEOUT_SECONDS * 20L));
    }

    /** 该玩家此刻是否在等输入（聊天监听器据此决定拦不拦消息）。 */
    public static boolean awaiting(Player player) {
        return player != null && Inputs.awaiting(player.getUniqueId());
    }

    /** 处理一条聊天输入，返回是否确实是这次输入的内容（不是就返回 false，消息照常走）。 */
    public static boolean submit(final Player player, String raw) {
        if (player == null) {
            return false;
        }
        final Inputs.Pending pending = Inputs.take(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (Inputs.Decision.of(raw) == Inputs.Decision.CANCEL) {
            Inputs.abandon(pending);
            return true;
        }
        final String text = raw == null ? "" : raw.trim();
        // 聊天事件是异步的，回调必须回主线程
        onMain(player, new Runnable() {
            @Override
            public void run() {
                pending.callback().value(text);
            }
        });
        return true;
    }

    /** 放弃这次输入并执行 onCancel。 */
    public static void cancel(Player player) {
        Inputs.Pending pending = player == null ? null : Inputs.take(player.getUniqueId());
        if (pending != null) {
            Inputs.abandon(pending);
        }
    }

    /** 丢弃状态且不执行任何回调（退服时用）。 */
    public static void forget(Player player) {
        if (player != null) {
            Inputs.forget(player.getUniqueId());
        }
    }

    // ---------- 内部实现 ----------

    /**
     * 回主线程执行；优先用 YLib 调度器，其次 Bukkit 调度器，都没有就同步执行。
     * <p>
     * 三级回退是为了「拿不到调度器」这件事本身不算失败：单元测试里没有服务端，
     * 退服清场时 Bukkit 可能已经关掉——这些情况下让回调照常发生，好过静默丢掉或抛 NPE。
     */
    private static void onMain(Player player, Runnable task) {
        UniversalScheduler scheduler = scheduler();
        if (scheduler != null) {
            scheduler.runLater(player, task, 1L);
            return;
        }
        YLib ylib = YLib.instance();
        Plugin plugin = ylib == null ? null : ylib.getPlugin();
        if (plugin != null) {
            try {
                Bukkit.getScheduler().runTask(plugin, task);
                return;
            } catch (Throwable noServer) {
                // 关服中服务端不再接任务：落到同步执行，别把回调吞掉
            }
        }
        task.run();
    }

    /** YLib 的调度器；YLib 没初始化或平台模块缺失都返回 null。 */
    private static UniversalScheduler scheduler() {
        YLib ylib = YLib.instance();
        if (ylib == null) {
            return null;
        }
        try {
            return ylib.getScheduler();
        } catch (RuntimeException e) {
            // 平台模块缺失时 getScheduler() 抛 YLibException；这里是尽力而为的能力，不该把调用方打断
            return null;
        }
    }

    /** 输入提示：说明填什么、当前值、怎么取消；一行一句，不依赖客户端把 \n 当换行渲染。 */
    private static void prompt(Player player, String title, String hint, String current) {
        player.sendMessage(TextRenderer.render(
                "&e请在聊天栏输入「" + title + "」的值&7（可填：" + (blank(hint) ? "自由文本" : hint) + "）"));
        player.sendMessage(TextRenderer.render("&7当前值：&f" + (blank(current) ? "（无）" : current)));
        player.sendMessage(TextRenderer.render("&7直接回车提交；输入 &f取消&7 放弃（留空表示清除）"));
    }

    /** Java 8 没有 String#isBlank。 */
    private static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
