package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 打开界面时的回调。
 * <p>
 * 与「渲染」的分工：渲染负责把界面画出来（每次 {@code refresh()} 都会跑），这里只表示
 * 「这个玩家看到了这个界面」，因此不会因为刷新内容而重复触发——「打开时记一笔」这类动作写在这里。
 */
@FunctionalInterface
public interface OpenHandler {

    void handle(Player viewer, Inventory inventory, MenuSession session);
}
