package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.SymbolState;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 一个符号的声明入口：{@code declare()} 里对同一符号的调用全部链在它身上。
 * <p>
 * 物品一律由 {@link #of(Items.Builder)} 或 {@link #pageItems} 提供，{@link #ph} / {@link #visible} /
 * {@link #onClick} 都是**修饰**——先说清「这一格长什么样」，再说「打开时填什么值、什么条件下显示、
 * 点了做什么」。
 * <p>
 * {@link #pageItems} 与 {@link #of} 互斥：一个符号要么是固定物品，要么是按页铺的条目区，
 * 两者同时声明就是语义冲突，因此会在声明时当场抛（见重构文档 §5 校验 15）。
 * <p>
 * <b>分页条目怎么响应点击</b>：条目区整体只有一份点击回调——回调拿到的 {@code slot} 就是被点的那一格，
 * 用 {@link MenuContext#indexInSymbol(Slot)} 换成页内下标即可定位到具体条目：
 * <pre>{@code
 * b.symbol('l').item("quest_entry")
 *     .pageItems((viewer, page, perPage) -> renderRows(viewer, page, perPage))
 *     .onClick((slot, ctx) -> open(ctx.page() * ctx.perPage() + ctx.indexInSymbol(slot)));
 * }</pre>
 */
public final class SymbolBinder {

    private final SymbolState state;

    SymbolBinder(SymbolState state) {
        this.state = state;
    }

    /**
     * yml 覆盖用的键。
     * <p>
     * 布局里的符号是「怎么画」，键是「配置里叫什么」——两者分开是为了让服主改配置时不必知道
     * 布局表里那个符号到底是一个字符还是 {@code `quests`} 这样的长名字。
     * <p>
     * 键与符号同名时不必调：不调时键默认就是符号名（如 {@code b.symbol('*')} 的键就是 {@code *}）。
     * 只有「单字符符号想要可读的配置键」这类解耦场景才需要写它。
     */
    public SymbolBinder item(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的配置键不能为空");
        }
        state.configKey(configKey);
        return this;
    }

    /** 固定物品的外观（名字与 lore 里可以含 {@code {占位符}}）。 */
    public SymbolBinder of(Items.Builder builder) {
        if (builder == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的 of(...) 传了 null");
        }
        if (state.isPaged()) {
            throw new MenuConfigException("符号 '" + state.symbol()
                    + "' 同时声明了 pageItems 与静态物品，语义冲突");
        }
        state.items(builder.build());
        return this;
    }

    /** 成品物品：不再解析占位符，也不再重新构造（每次渲染给的都是这一份的克隆）。 */
    public SymbolBinder of(ItemStack stack) {
        return of(Items.of(stack));
    }

    /**
     * 打开时求值的占位符。
     * <p>
     * <b>求值时机是「每次打开 / 刷新」，不是构建期</b>——构建期冻结的数值（任务数、页码、余额）
     * 永远不会更新，而那正是菜单上最需要变的那部分。
     */
    public SymbolBinder ph(String name, Function<MenuContext, String> value) {
        if (value == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的占位符 '" + name + "' 取值函数是 null");
        }
        if (state.isPaged()) {
            throw new MenuConfigException("符号 '" + state.symbol()
                    + "' 是分页符号：条目由数据决定，不能再声明占位符 '" + name + "'");
        }
        state.placeholder(name, value);
        return this;
    }

    /** {@code false} → 该符号的所有槽位回落到 {@code fillEmpty}。 */
    public SymbolBinder visible(Predicate<MenuContext> predicate) {
        if (predicate == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的 visible(...) 传了 null");
        }
        state.visible(predicate);
        return this;
    }

    /** 全量兜底回调：任何没被更精确规则接住的点击都走它。优先级最低。 */
    public SymbolBinder onClick(ClickHandler handler) {
        requireHandler(handler);
        state.fallbackClick(handler);
        return this;
    }

    /** 精确指定键位。带 Shift 的组合走单独一张表，因此这里注册的是「不按修饰键」的那一种。 */
    public SymbolBinder onClick(ClickKind kind, ClickHandler handler) {
        if (kind == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的点击键位是 null");
        }
        requireHandler(handler);
        state.kindClick(kind, handler);
        return this;
    }

    /** 按 Bukkit 槽位号指定回调：同一个符号想在不同格子上做不同事时用，优先级最高。 */
    public SymbolBinder onClick(Map<Integer, ClickHandler> bySlot) {
        if (bySlot == null) {
            return this;
        }
        for (Map.Entry<Integer, ClickHandler> entry : bySlot.entrySet()) {
            requireHandler(entry.getValue());
            state.slotClick(entry.getKey().intValue(), entry.getValue());
        }
        return this;
    }

    /** 便捷入口：左键（不含 Shift+左键，那一个走 {@link #onShiftLeftClick}）。 */
    public SymbolBinder onLeftClick(ClickHandler handler) {
        return onClick(ClickKind.LEFT, handler);
    }

    /** 便捷入口：右键。 */
    public SymbolBinder onRightClick(ClickHandler handler) {
        return onClick(ClickKind.RIGHT, handler);
    }

    /** 便捷入口：Shift+左键。 */
    public SymbolBinder onShiftLeftClick(ClickHandler handler) {
        requireHandler(handler);
        state.shiftClick(ClickKind.LEFT, handler);
        return this;
    }

    /** 便捷入口：Shift+右键。 */
    public SymbolBinder onShiftRightClick(ClickHandler handler) {
        requireHandler(handler);
        state.shiftClick(ClickKind.RIGHT, handler);
        return this;
    }

    /**
     * 把这个符号变成**分页条目区**：每页条数 = 该符号在布局里出现的格数。
     *
     * @param fetcher 按页取数据：{@code (viewer, page, perPage) -> 这一页的条目}；
     *                {@code perPage} 由框架给（布局决定），调用方不该另写常量
     */
    public SymbolBinder pageItems(Fetcher fetcher) {
        if (fetcher == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的 pageItems 数据源是 null");
        }
        return pageItems(new PageSource() {
            @Override
            public List<ItemStack> fetch(org.bukkit.entity.Player viewer, int page, int perPage) {
                return fetcher.fetch(viewer, page, perPage);
            }

            /** 便捷写法不报总条数：{@code pageCount()} 因此是「未知」。 */
            @Override
            public int total(org.bukkit.entity.Player viewer) {
                return -1;
            }
        });
    }

    /**
     * 同上，但由数据源自己报告总条数（{@link PageSource#total}）。
     * <p>
     * 只有需要「页码指示器显示真实总页数」或「用总页数判断能不能翻」时才用它；
     * 只要「取第几页」的话 {@link #pageItems(Fetcher)} 更短。
     */
    public SymbolBinder pageItems(PageSource source) {
        if (source == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的 pageItems 数据源是 null");
        }
        if (state.items() != null) {
            throw new MenuConfigException("符号 '" + state.symbol()
                    + "' 同时声明了 pageItems 与静态物品，语义冲突");
        }
        state.pageItems(source);
        return this;
    }

    /**
     * 快捷：上一页按钮（到底了就什么都不做）。
     * <p>
     * 等价于 {@code onClick((slot, ctx) -> { if (ctx.page() > 0) { ctx.page(ctx.page() - 1); ctx.menu().refresh(); } })}。
     * 边界判断在业务侧完成：框架不知道总页数（游标式数据源的 {@code total()} 返回 -1）。
     */
    public SymbolBinder previousPage() {
        return onClick(new ClickHandler() {
            @Override
            public void handle(Slot slot, MenuContext ctx) {
                if (ctx.page() > 0) {
                    ctx.page(ctx.page() - 1);
                    ctx.menu().refresh();
                }
            }
        });
    }

    /**
     * 快捷：下一页按钮。
     * <p>
     * <b>不要靠「返回空列表」猜到底</b>——空列表也可能只是这一页恰好没数据（筛选后），把它当成
     * 「没有下一页」会让玩家永远翻不过去。因此判断顺序是：
     * 总页数已知（{@code pageCount() >= 0}）就用它；未知时退化为「这一页取满了 {@code perPage} 条
     * 即还有下一页」（{@link MenuContext#pageSize()} 是这一页**实际取到**的条数，不是格子数）。
     */
    public SymbolBinder nextPage() {
        return onClick(new ClickHandler() {
            @Override
            public void handle(Slot slot, MenuContext ctx) {
                int next = ctx.page() + 1;
                int total = ctx.pageCount();
                boolean allowed = total >= 0 ? next < total : ctx.pageSize() >= ctx.perPage();
                if (allowed) {
                    ctx.page(next);
                    ctx.menu().refresh();
                }
            }
        });
    }

    /** 页码指示器：{@code {page}} 是 1 起的当前页，{@code {pages}} 是总页数（未知时 {@code ?}）。 */
    public SymbolBinder pageIndicator() {
        return ph("page", new Function<MenuContext, String>() {
            @Override
            public String apply(MenuContext ctx) {
                return String.valueOf(ctx.page() + 1);
            }
        }).ph("pages", new Function<MenuContext, String>() {
            @Override
            public String apply(MenuContext ctx) {
                int count = ctx.pageCount();
                return count < 0 ? "?" : String.valueOf(count);
            }
        });
    }

    /** 这个符号声明的槽位（由 {@code MenuBuilder.build()} 回填后才有值）。 */
    List<Slot> slots() {
        return state.slots();
    }

    SymbolState state() {
        return state;
    }

    private void requireHandler(ClickHandler handler) {
        if (handler == null) {
            throw new MenuConfigException("符号 '" + state.symbol() + "' 的点击回调是 null：不想响应就不要注册");
        }
    }
}
