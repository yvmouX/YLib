package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 关闭界面时的回调。
 * <p>
 * <b>每次打开只触发一次</b>：客户端强制关闭界面时原版会发两次关闭事件，库负责把它们压成一次，
 * 因此这里可以放心做发奖、扣费这类不能重复的动作。
 * <p>
 * 第三个参数是关闭原因（{@code InventoryCloseEvent.Reason}）。它的类型写成 {@link Object}
 * 而不是那个枚举是刻意的：{@code Reason} 是较新版本才有的 API，写进方法签名会把本库的
 * 编译基线抬上去，而「为什么关的」对绝大多数回调并不重要。需要用到时自己取名字：
 * <pre>{@code
 * menu.onClose((viewer, inventory, reason, session) -> {
 *     if (String.valueOf(reason).contains("PLAYER")) { ... }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface CloseHandler {

    void handle(Player viewer, Inventory inventory, Object reason, MenuSession session);
}
