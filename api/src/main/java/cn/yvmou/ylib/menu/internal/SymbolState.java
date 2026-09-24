package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.ClickHandler;
import cn.yvmou.ylib.menu.ClickKind;
import cn.yvmou.ylib.menu.Items;
import cn.yvmou.ylib.menu.MenuConfigException;
import cn.yvmou.ylib.menu.MenuContext;
import cn.yvmou.ylib.menu.PageSource;
import cn.yvmou.ylib.menu.Slot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 一个符号**声明出来的全部内容**：物品、占位符、显隐、点击回调、分页数据源。
 * <p>
 * 这是 {@code declare()} 的产物，构建期产出后就不再改动（{@code declare()} 只在注册期跑一次）。
 * 因此这里只存「怎么做」，不存「这次的结果」——占位符存的是函数而不是字符串，物品存的是
 * {@link Items} 而不是 {@link ItemStack}，正是为了在每次打开时重新求值 / 重新构造。
 * <p>
 * 名字与 lore 存在这里的是**模板原文**（含 {@code {名字}}），求值发生在渲染期。
 */
public final class SymbolState {

    /** 符号在布局里写的那个名字（单字符或多字符）。 */
    private final String symbol;

    /** yml 覆盖用的键；没写 {@code item(...)} 时为符号本身。 */
    private String configKey;

    /** 静态物品（与 {@link #pageSource} 互斥）。 */
    private Items items;

    /** 分页数据源；不是分页符号时为 {@code null}。 */
    private PageSource pageSource;

    /** 分页符号每页条数。 */
    private int perPage;

    /** 占位符：名字 → 打开时求值的函数。 */
    private final Map<String, Function<MenuContext, String>> placeholders =
            new LinkedHashMap<String, Function<MenuContext, String>>();

    /** 显隐判断；{@code null} 表示总是显示。 */
    private Predicate<MenuContext> visible;

    /** 全量兜底回调，优先级最低。 */
    private ClickHandler fallbackClick;

    /** 精确键位回调。 */
    private final Map<ClickKind, ClickHandler> kindClicks = new EnumMap<ClickKind, ClickHandler>(ClickKind.class);

    /** Shift+键位回调（只在单字符符号允许的范围内用两个组合）。 */
    private final Map<ClickKind, ClickHandler> shiftClicks = new EnumMap<ClickKind, ClickHandler>(ClickKind.class);

    /** 按 Bukkit 槽位号指定的回调，优先级最高。 */
    private final Map<Integer, ClickHandler> slotClicks = new LinkedHashMap<Integer, ClickHandler>();

    /** 这个符号占据的槽位；由 {@code MenuBuilder.build()} 在解析布局后回填。 */
    private List<Slot> slots = Collections.emptyList();

    /** 行为是否由 yml 的 {@code actions} 接管。 */
    private boolean actionsBound;

    public SymbolState(String symbol) {
        this.symbol = symbol;
        this.configKey = symbol;
    }

    // ---------- 声明（构建期） ----------

    public void configKey(String key) {
        this.configKey = key;
    }

    public void items(Items items) {
        this.items = items;
    }

    /**
     * yml 的物品覆盖：用合并后的 {@link Items} 换掉代码里那一份。
     * <p>
     * 合并规则由 {@code MenuConfigLoader} 负责（yml 写了的字段以 yml 为准、缺的回落代码默认值），
     * 到这里时已经是最终结果。
     */
    public void overrideItems(Items items) {
        this.items = items;
    }

    public void pageItems(int perPage, PageSource source) {
        this.perPage = perPage;
        this.pageSource = source;
    }

    public void placeholder(String name, Function<MenuContext, String> value) {
        if (name == null || name.trim().isEmpty()) {
            throw new MenuConfigException("占位符名不能为空（符号 '" + symbol + "'）");
        }
        placeholders.put(name, value);
    }

    public void visible(Predicate<MenuContext> predicate) {
        this.visible = predicate;
    }

    public void fallbackClick(ClickHandler handler) {
        this.fallbackClick = handler;
    }

    /**
     * yml 里配了 {@code actions} 时用它接管点击：把声明式动作包成一个 {@code ClickHandler} 存进来。
     * <p>
     * 走 {@code fallbackClick}（优先级最低的那一档）是刻意的：声明式动作既然是「这一格点了做什么」的
     * 完整定义，它就应当在所有更精确的规则都不成立时生效；而 yml 一旦配了 actions，
     * 代码侧的 {@code .onClick(...)} 本来就被要求忽略（见 {@code MenuConfigLoader}）。
     */
    void actionClick(ClickHandler handler) {
        this.fallbackClick = handler;
        this.actionsBound = true;
    }

    /** 这一格的行为是否由 yml 的 {@code actions} 接管（用来提示代码里那些被忽略的 {@code onClick}）。 */
    public boolean actionsBound() {
        return actionsBound;
    }

    public void kindClick(ClickKind kind, ClickHandler handler) {
        kindClicks.put(kind, handler);
    }

    public void shiftClick(ClickKind kind, ClickHandler handler) {
        shiftClicks.put(kind, handler);
    }

    public void slotClick(int slot, ClickHandler handler) {
        slotClicks.put(Integer.valueOf(slot), handler);
    }

    /** 回填这个符号占据的槽位；只在 {@code MenuBuilder.build()} 里调一次。 */
    public void slots(List<Slot> slots) {
        this.slots = Collections.unmodifiableList(new ArrayList<Slot>(slots));
    }

    // ---------- 读取（校验与渲染） ----------

    public String symbol() {
        return symbol;
    }

    public String configKey() {
        return configKey;
    }

    public Items items() {
        return items;
    }

    public PageSource pageSource() {
        return pageSource;
    }

    public int perPage() {
        return perPage;
    }

    public Map<String, Function<MenuContext, String>> placeholders() {
        return Collections.unmodifiableMap(placeholders);
    }

    public Predicate<MenuContext> visible() {
        return visible;
    }

    public List<Slot> slots() {
        return slots;
    }

    public boolean isPaged() {
        return pageSource != null;
    }

    // ---------- 回调解析（渲染期之后、点击时） ----------

    /**
     * 按优先级挑出这次点击该跑的回调，挑不到返回 {@code null}。
     * <p>
     * 优先级（见重构文档 §7.2 / §11）：
     * <ol>
     *   <li>按槽位号精确指定的回调；</li>
     *   <li>精确 {@code ClickKind} 匹配（带 Shift 的走另一张表，因此 {@code Shift+左键} 与
     *       {@code 左键} 是两件不同的事）；</li>
     *   <li>便捷入口注册的回调（{@code onLeftClick} 等，落在同一张 kind 表里，写在前面）；</li>
     *   <li>{@code onClick} 全量兜底；</li>
     *   <li>都没有 → {@code null}，由调用方按 {@code interaction} 策略处理。</li>
     * </ol>
     */
    public ClickHandler resolve(Slot slot, cn.yvmou.ylib.menu.ClickType clickType) {
        if (slot != null) {
            ClickHandler bySlot = slotClicks.get(Integer.valueOf(slot.index()));
            if (bySlot != null) {
                return bySlot;
            }
        }
        if (clickType != null) {
            Map<ClickKind, ClickHandler> table = clickType.shift() ? shiftClicks : kindClicks;
            ClickHandler exact = table.get(clickType.kind());
            if (exact != null) {
                return exact;
            }
            // Shift 组合没单独注册时，退化到「不带修饰键」的那一个：Shift+左键没写处理就当普通左键
            if (clickType.shift()) {
                ClickHandler plain = kindClicks.get(clickType.kind());
                if (plain != null) {
                    return plain;
                }
            }
        }
        return fallbackClick;
    }

    /** 有没有任何点击回调；没有的话这一格等同于空槽（点击只按 interaction 策略取消）。 */
    public boolean hasClick() {
        return fallbackClick != null || !kindClicks.isEmpty() || !shiftClicks.isEmpty() || !slotClicks.isEmpty();
    }
}
