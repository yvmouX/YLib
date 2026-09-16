package cn.yvmou.ylib.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * 把聊天栏里那句话喂给 {@link PlayerInput}；由 {@link MenuListener#init(Plugin)} 一并注册，宿主不必知道它。
 * <p>
 * 用 Spigot 的 {@link AsyncPlayerChatEvent}：Paper 的 AsyncChatEvent 不在 spigot-api 里，用了会在 Spigot 上加载失败。
 */
public final class InputListener implements Listener {

    /** 只注册一次：宿主可能在自己的 onEnable 重载时又调一次 init，重复注册会让服务端报错。 */
    private static boolean registered;

    /** 注册聊天输入拦截（单独调也可以，正常路径是 {@link MenuListener#init(Plugin)}）。 */
    public static void init(Plugin plugin) {
        if (registered) {
            return;
        }
        registered = true;
        Bukkit.getPluginManager().registerEvents(new InputListener(), plugin);
    }

    /**
     * LOWEST 优先且不忽略已取消事件：等待输入期间那句话必须被拦下，否则会漏进公屏。
     * 先取消再取值——反过来的话，取值期间别的监听器可能已经放行，消息照样漏出去。
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!PlayerInput.awaiting(player)) {
            return;
        }
        event.setCancelled(true);
        // 事件是异步的，回调切主线程由 PlayerInput 内部负责
        PlayerInput.submit(player, event.getMessage());
    }

    /** 退服的玩家不再等输入：留着状态会让下一句聊天被吞掉。 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerInput.forget(event.getPlayer());
    }
}
