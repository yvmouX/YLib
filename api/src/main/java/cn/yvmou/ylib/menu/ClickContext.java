package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.SymbolState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Collections;
import java.util.List;

/**
 * 点击发生时的 {@link MenuContext}：与渲染期的 {@code MenuView} 实现同一个接口，但多了
 * 「被点的是哪一格、怎么点的、原始事件是什么」。
 * <p>
 * 两处共用同一个接口是刻意的——使用者只学一套 {@code ctx} 的方法，占位符里能读的东西
 * 点击回调里照样能读（{@code session()}、{@code page()} 这些在两边都成立）。
 * <p>
 * 「当前符号」由派发层在调用回调前设好：点击落在哪一格，那一格的符号就是当前符号。
 */
public final class ClickContext implements MenuContext {

    private final MenuView view;
    private final InventoryClickEvent event;
    private final ClickType clickType;
    private final Slot slot;
    private final SymbolState symbol;

    public ClickContext(MenuView view, InventoryClickEvent event, ClickType clickType, Slot slot,
                        SymbolState symbol) {
        this.view = view;
        this.event = event;
        this.clickType = clickType;
        this.slot = slot;
        this.symbol = symbol;
    }

    @Override
    public Player viewer() {
        return view.viewer();
    }

    @Override
    public ClickType clickType() {
        return clickType;
    }

    @Override
    public InventoryClickEvent event() {
        return event;
    }

    @Override
    public MenuControl menu() {
        return view;
    }

    @Override
    public MenuSession session() {
        return view.session();
    }

    @Override
    public List<Slot> symbolSlots() {
        return symbol == null ? Collections.<Slot>emptyList() : symbol.slots();
    }

    @Override
    public Slot slot() {
        return slot;
    }

    @Override
    public int page() {
        return view.session().page();
    }

    @Override
    public void page(int page) {
        view.session().page(page);
    }

    @Override
    public int perPage() {
        if (symbol == null) {
            return 0;
        }
        return symbol.isPaged() ? symbol.perPage() : symbol.slots().size();
    }

    @Override
    public int pageSize() {
        return view.pageSizeOf(symbol);
    }

    @Override
    public int pageCount() {
        return view.pageCountOf(symbol);
    }

    /** 槽位 → 页内下标。分页条目的全局下标是 {@code page() * perPage() + indexInSymbol(slot)}。 */
    @Override
    public int indexInSymbol(Slot target) {
        if (symbol == null || target == null) {
            return -1;
        }
        List<Slot> slots = symbol.slots();
        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).index() == target.index()) {
                return index;
            }
        }
        return -1;
    }

    /** 这一格所属的符号；兜底填充物（没有符号）时为 {@code null}。 */
    public SymbolState symbol() {
        return symbol;
    }

    /** 这一格注册的点击回调；{@code null} 表示没注册。 */
    public ClickHandler handler() {
        return symbol == null ? null : symbol.resolve(slot, clickType);
    }
}
