package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 点击动作的上下文：作为**宿主实现的 {@link MenuActions} 的参数**传进来。
 * <p>
 * 与 {@link MenuContext} 分开是刻意的：yml 里的声明式动作不是 lambda——它由宿主集中实现一次、
 * 供所有菜单复用。因此它只该拿到「谁点的、哪个菜单、第几页、原始事件」这几样，
 * 而不该拿到 {@code visible()} / 占位符求值那套只在渲染期成立的上下文。
 */
public interface ActionContext {

    /** 点下这一格的玩家。 */
    Player viewer();

    /** 玩家在这个菜单上的持久状态（页码、筛选条件）。 */
    MenuSession session();

    /** 当前页，0 起。 */
    int page();

    /** 写入当前页。 */
    void page(int page);

    /** 原始点击事件：需要读光标物品、点击的格子这类信息时用。 */
    InventoryClickEvent event();

    /** 关掉这个界面。 */
    void close();

    /** 原地重渲染。 */
    void refresh();
}
