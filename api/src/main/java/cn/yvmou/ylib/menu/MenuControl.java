package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;

/**
 * 回调里能对**菜单本身**做的事。
 * <p>
 * 单独抽出来而不是把方法摊在 {@link MenuContext} 上：{@code ctx.menu().close()} 能一眼看出
 * 「这是对界面整体动手」，而 {@code ctx.close()} 会和 {@code ctx.viewer().closeInventory()} 混在一起。
 */
public interface MenuControl {

    /** 打开界面的玩家。 */
    Player viewer();

    /** 关掉这个界面（玩家按 ESC 也是同样效果）。 */
    void close();

    /**
     * 原地重渲染：清空后重跑渲染，用于翻页与「改完数据刷新一下」。
     * <p>
     * 与 {@link #open(Player)} 的区别是**不重建容器**——翻页只用 {@code refresh()}，
     * 用 {@code open()} 会重建背包（闪屏、光标上的物品被丢掉）。
     */
    void refresh();

    /** 给某个玩家打开这个菜单，并返回他在这个菜单上的 session。 */
    MenuSession open(Player viewer);

    /** 这个玩家此刻是否开着这个菜单。 */
    boolean isOpen(Player viewer);

    /** 这个菜单的静态定义（布局、符号、物品声明）。 */
    MenuDefinition definition();

    /** 清空某个玩家在这个菜单上的持久状态（页码、筛选条件），下次打开回到初始状态。 */
    void resetSession(Player viewer);
}
