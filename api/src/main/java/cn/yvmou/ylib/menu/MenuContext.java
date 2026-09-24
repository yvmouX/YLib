package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * 回调上下文：点击回调、占位符求值、{@code visible()} 判断都用它。
 * <p>
 * 三处共用同一个接口是刻意的——它们要的东西高度重叠（谁在看、第几页、这个符号占哪些格），
 * 分成三个接口只会让「占位符里想读 session」这类需求落空。
 * <p>
 * <b>渲染期的字段是 {@code null} / 空</b>：占位符在「打开 / 刷新」时求值，那时既没有点击也没有事件，
 * 因此 {@link #clickType()} 与 {@link #event()} 返回 {@code null}，{@link #slot()} 返回 {@code null}。
 * 写占位符时不要依赖它们——它们只在点击回调里有值。
 */
public interface MenuContext {

    /** 打开这个界面的玩家。 */
    Player viewer();

    /** 这次点击的类型；渲染期（占位符求值）为 {@code null}。 */
    ClickType clickType();

    /** 原始点击事件，给「需要读光标物品 / 点击的格子」的回调；渲染期为 {@code null}。 */
    InventoryClickEvent event();

    /** 对界面整体的操作（close / refresh / open）。 */
    MenuControl menu();

    /** **跨「打开」的持久状态**（页码、筛选条件）。{@code close()} 不会销毁它，见 {@link MenuSession}。 */
    MenuSession session();

    /**
     * 当前符号占据的全部槽位，按布局的阅读顺序（先上后下、先左后右）。
     * <p>
     * 渲染期与点击回调里都有值——{@link #indexInSymbol(Slot)} 就是靠它的顺序换算的。
     */
    List<Slot> symbolSlots();

    /** 点击落在哪个槽位；渲染期为 {@code null}。 */
    Slot slot();

    // ---------- 分页便捷入口（等价于 session() 里对应方法，少写一层） ----------

    /** 当前页，0 起。 */
    int page();

    /** 写入当前页；只校验 {@code >= 0}，上界由 {@link #pageCount()} 决定。 */
    void page(int page);

    /** 当前符号声明的每页条数；当前符号不是分页符号时为该符号的槽位数。 */
    int perPage();

    /** 总页数；数据源{@code total()} 返回 -1（未知）时也返回 -1。 */
    int pageCount();

    /**
     * 槽位 → **页内下标**。
     * <p>
     * 等价于 {@code symbolSlots().indexOf(slot)}，但由框架直接算出，使用者不必自己反查——
     * 分页条目的全局下标是 {@code page() * perPage() + indexInSymbol(slot)}。
     */
    int indexInSymbol(Slot slot);
}
