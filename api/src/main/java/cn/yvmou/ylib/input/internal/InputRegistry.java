package cn.yvmou.ylib.input.internal;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.scheduler.UniversalScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 聊天输入的 Bukkit 一侧：拦聊天、管生命周期，把回调与容器操作派到玩家所属的调度线程。
 *
 * <h2>线程契约的落点</h2>
 * 聊天事件是异步的，回调却要关/开界面、改草稿；而容器操作在 Folia/Canvas 上只允许玩家
 * 所属区域线程做。所以这里的一切出站动作都经 {@link #dispatch} 按实体调度——
 * Folia/Canvas 上落到玩家区域线程，Paper/Spigot 上就是主线程任务。
 */
public final class InputRegistry implements Listener {

    /** 宿主显式注入的装配；未注入时回落到 {@link YLib#instance()} 的（尽力而为）。 */
    private static volatile Plugin plugin;
    private static volatile UniversalScheduler scheduler;
    private static volatile boolean listenerRegistered;

    InputRegistry() {
    }

    /** 宿主装配：事件派发登记一次即可；单测里没有服务端，注册不了不算失败。 */
    public static void init(Plugin owner, UniversalScheduler injected) {
        if (owner == null) {
            throw new IllegalArgumentException("ChatInput.init 的 plugin 是 null");
        }
        plugin = owner;
        scheduler = injected;
        if (listenerRegistered) {
            return;
        }
        try {
            Bukkit.getPluginManager().registerEvents(new InputRegistry(), owner);
            listenerRegistered = true;
        } catch (RuntimeException noServer) {
            // 单测里没有服务端：注册不了不该成为失败原因（状态机本身不需要监听器）
        }
    }

    /**
     * 开始一次提问：登记等待，再把「关界面 + 发问」派到玩家所属线程。
     * 登记在派发之前：问出去的那一瞬起，答案就已经有地方去了。
     */
    public static void ask(Player player, String prompt, Consumer<String> onAnswer, Runnable onCancel, long timeoutMs) {
        Inputs.put(new Inputs.Pending(player.getUniqueId(), onAnswer, onCancel,
                System.currentTimeMillis() + timeoutMs));
        dispatch(player, () -> {
            try {
                player.closeInventory();
            } catch (Throwable noServer) {
                // 关服中或单测环境：关不掉也不该打断这次提问
            }
            player.sendMessage(prompt);
        });
    }

    /** 玩家此刻挂着的提问作废，不跑任何回调（菜单重开、玩家改主意时调）。 */
    public static void cancel(UUID playerId) {
        Inputs.take(playerId);
    }

    /** 宿主禁用：全部作废，并把监听器标记复位（Bukkit 会随禁用注销监听器，再装配要能重新登记）。 */
    public static void shutdown() {
        Inputs.clear();
        listenerRegistered = false;
    }

    /** 抢在其它监听器之前拿走这句话：{@code LOWEST} 保证看到的还是原文。 */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Inputs.Pending pending = Inputs.take(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        // 这句话是给提问的，不再外传（否则全服都会看见玩家填的内容）
        event.setCancelled(true);
        String raw = event.getMessage();
        if (Inputs.Decision.of(raw, pending.deadline()) == Inputs.Decision.CANCEL) {
            if (pending.onCancel() != null) {
                dispatch(event.getPlayer(), pending.onCancel());
            }
            return;
        }
        if (pending.onAnswer() != null) {
            String answer = raw == null ? "" : raw.trim();
            dispatch(event.getPlayer(), () -> pending.onAnswer().accept(answer));
        }
    }

    /** 退服：提问作废，不回调（那时玩家已经不在，开界面只会报错）。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Inputs.take(event.getPlayer().getUniqueId());
    }

    /**
     * 出站动作（回调、关界面、发问）的唯一出口：一律按实体调度落到玩家所属线程。
     * 装配缺失（单测、YLib 未初始化）时就地执行——意图不该因为拿不到调度器而消失，
     * 与菜单包 {@code MenuScheduler} 的回退是同一个道理。
     */
    static void dispatch(Player player, Runnable action) {
        if (action == null) {
            return;
        }
        Plugin owner = pluginOf();
        UniversalScheduler s = schedulerOf();
        if (owner == null || s == null) {
            action.run();
            return;
        }
        s.runTask(owner, player, action);
    }

    private static Plugin pluginOf() {
        Plugin bound = plugin;
        if (bound != null) {
            return bound;
        }
        YLib lib = YLib.instance();
        return lib == null ? null : lib.getPlugin();
    }

    private static UniversalScheduler schedulerOf() {
        UniversalScheduler bound = scheduler;
        if (bound != null) {
            return bound;
        }
        // init 未显式注入时按 YLib.instance() 的约定兜底：拿不到只说明「没有服务端」
        YLib lib = YLib.instance();
        if (lib == null) {
            return null;
        }
        try {
            return lib.getScheduler();
        } catch (Throwable unavailable) {
            return null;
        }
    }
}
