package cn.yvmou.ylib.menu;

/**
 * 菜单的**事件策略**：玩家能对菜单里的物品做什么。
 * <p>
 * 默认 {@link #READ_ONLY}。这条默认是刻意的：不显式声明可修改，玩家就不能动菜单里的任何物品——
 * 「上线后发现玩家能把菜单物品拖进背包」是这类框架最经典的事故，而它只需要一个默认值就能避免。
 */
public enum Interaction {

    /**
     * 只读（默认）：取消所有点击、拖拽、漏斗抽取与丢入。
     * <p>
     * 拦的不只是 {@code InventoryClickEvent}（见 {@code MenuDispatcher}）：拖拽、漏斗/其他容器抽走、
     * 玩家丢弃的物品落到菜单上都要各拦一道，只拦点击会被这三种绕过去。
     * <p>
     * 声明了 {@link SymbolBinder#onItemDrop} 的格子不受这条影响：那一格本来就是用来**接收**的，
     * 交给它的物品既不会被放进槽里、也不会留在菜单里（见 {@link ItemDropHandler}）。
     */
    READ_ONLY,

    /** 允许玩家自由整理（仅适合「仓库 / 背包」类菜单）。 */
    ALLOW_MOVE,

    /** 只允许拿走，不允许放入。 */
    ALLOW_TAKE
}
