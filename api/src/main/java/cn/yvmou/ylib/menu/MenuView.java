package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuLog;
import cn.yvmou.ylib.menu.internal.MenuRegistry;
import cn.yvmou.ylib.menu.internal.MenuScheduler;
import cn.yvmou.ylib.menu.internal.SymbolState;
import cn.yvmou.ylib.utils.LegacyCodes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 一次「打开」的**渲染结果与会话上下文**：一个玩家 + 一个菜单 = 一个 {@code MenuView}。
 *
 * <h2>职责</h2>
 * <ul>
 *   <li>渲染：求值占位符 → 构造全新的 {@link ItemStack} → 写进容器；</li>
 *   <li>归属：充当 {@link InventoryHolder}，派发层靠**对象身份**而不是标题认出自己的容器
 *       （标题会随语言 / 占位符变，也可能与别的插件撞名）；</li>
 *   <li>上下文：实现 {@link MenuContext}，占位符求值读到的就是它；</li>
 *   <li>控制：实现 {@link MenuControl}，提供 {@code close() / refresh() / open()}。</li>
 * </ul>
 *
 * <h2>「当前符号」为什么是一个字段</h2>
 * 占位符函数只收一个 {@link MenuContext}，而 {@code {page}} / {@code {pages}} 这类值取决于
 * <b>是哪个符号</b>在求值。渲染是单线程、逐格推进的，因此渲染每一格前把「当前符号」设成那一格的符号，
 * 占位符函数就能通过 {@link #perPage()} / {@link #pageCount()} 读到正确的值——这比给每个占位符函数
 * 多传一个符号参数要好用得多（使用者写的是 {@code ctx -> ...}）。
 */
public final class MenuView implements MenuControl, MenuContext, InventoryHolder {

    /**
     * 这个菜单的静态定义。
     * <p>
     * 不是 {@code final}：热重载会用 {@link #reuse(MenuDefinition)} 把它换成新的一份，
     * 这样已经开着的界面不必关掉重开（容器大小相同时）。
     */
    private MenuDefinition definition;
    private final Player viewer;
    private final MenuSession session;
    private final Inventory inventory;

    /** 槽位 → 渲染结果。以它而不是容器内容为准：从容器取出来的 ItemStack 已被 Bukkit 复制过。 */
    private final Map<Integer, Rendered> rendered = new HashMap<Integer, Rendered>();

    /** 本次渲染里每个符号只取一次数据（分页符号占多格，逐格取会让同一页查 N 次）。 */
    private final Map<String, List<ItemStack>> pageCache = new HashMap<String, List<ItemStack>>();

    /** 正在渲染的符号；占位符求值期间才有值。 */
    private SymbolState currentSymbol;

    private boolean rendering;

    /** 已经排了一个「下一 tick 重画」的任务；{@code delayedRefresh} 下用来把多次请求合并成一次。 */
    private boolean refreshScheduled;

    private MenuView(MenuDefinition definition, Player viewer, MenuSession session, String title) {
        this.definition = definition;
        this.viewer = viewer;
        this.session = session;
        this.inventory = Bukkit.createInventory(this, definition.size(), title);
    }

    /** 建一个渲染目标并把内容画出来。 */
    public static MenuView create(MenuDefinition definition, Player viewer, MenuSession session,
                                  Map<String, String> titleVars) {
        String title = Placeholders.apply(definition.template().title(), titleVars);
        MenuView view = new MenuView(definition, viewer, session, LegacyCodes.toSection(title));
        view.draw();
        return view;
    }

    /**
     * 换一份定义并重画，用于热重载：把这个玩家开着的界面切到新定义上。
     *
     * @return 换成功了吗；{@code false} 表示新定义与当前容器**大小不一致**
     *         （布局行数改了），那时只能关掉重开——容器大小在 {@code createInventory} 之后就定稿了。
     */
    public boolean reuse(MenuDefinition replacement) {
        if (replacement == null || replacement.size() != definition.size()) {
            return false;
        }
        this.definition = replacement;
        draw();
        return true;
    }

    // ---------- 渲染 ----------

    /** 重建容器内容。首次打开与 {@link MenuControl#refresh()} 都走它。 */
    private void draw() {
        if (rendering) {
            // 「动作里刷新、渲染里又刷新」这类写法若没有这道闸门就是无限递归
            return;
        }
        rendering = true;
        try {
            inventory.clear();
            rendered.clear();
            pageCache.clear();
            for (int index = 0; index < definition.size(); index++) {
                Rendered result = buildSlot(index);
                if (result == null) {
                    fillEmpty(index);
                } else {
                    inventory.setItem(index, result.stack());
                    rendered.put(Integer.valueOf(index), result);
                }
            }
        } finally {
            rendering = false;
            currentSymbol = null;
        }
    }

    /**
     * 算出某一格该摆什么；返回 {@code null} 表示这一格回落到 {@code fillEmpty}
     * （空位、没有设备符号、{@code visible()} 为 false、符号没给物品）。
     */
    private Rendered buildSlot(int index) {
        String symbol = definition.symbolAt(index);
        if (symbol == null) {
            return null;
        }
        SymbolState state = definition.stateOf(symbol);
        if (state == null) {
            return null;
        }
        currentSymbol = state;
        try {
            if (state.visible() != null && !state.visible().test(this)) {
                return null;
            }
            ItemStack stack;
            if (state.isPaged()) {
                stack = pagedStack(state, index);
            } else if (state.items() != null) {
                stack = staticStack(state);
            } else {
                return null;
            }
            Slot slot = slotAt(state, index);
            return new Rendered(stack, state, slot, state.resolve(slot, null));
        } finally {
            currentSymbol = null;
        }
    }

    /** 静态物品：每次都要重新求值占位符、重新构造——绝不复用上一次的实例。 */
    private ItemStack staticStack(SymbolState state) {
        Items items = state.items();
        Map<String, String> values = placeholders(state);
        String name = Placeholders.apply(items.name(), values);
        List<String> lore = items.lore() == null ? null : Placeholders.apply(items.lore(), values);
        return items.render(name, lore);
    }

    /** 分页条目：按**槽位在该符号里的下标**取这一页的第 N 条。 */
    private ItemStack pagedStack(SymbolState state, int index) {
        List<ItemStack> page = pageOf(state);
        int position = positionIn(state, index);
        if (position < 0 || position >= page.size()) {
            // 条目不足 → 显式写空气。绝不能沿用上一次渲染的残留：上一页的任务留在屏幕上，
            // 玩家点它会打开一个不属于本页的任务
            return new ItemStack(Material.AIR);
        }
        ItemStack stack = page.get(position);
        return stack == null ? new ItemStack(Material.AIR) : stack;
    }

    /** 取这个分页符号当前页的条目；一次渲染只取一次。 */
    private List<ItemStack> pageOf(SymbolState state) {
        String symbol = state.symbol();
        List<ItemStack> cached = pageCache.get(symbol);
        if (cached != null) {
            return cached;
        }
        List<ItemStack> fetched;
        try {
            fetched = state.pageSource().fetch(viewer, session.page());
        } catch (RuntimeException failure) {
            // 取数失败不该让整个界面开不出来；数据源自己该记日志
            fetched = Collections.emptyList();
        }
        if (fetched == null) {
            fetched = Collections.emptyList();
        }
        int perPage = state.perPage();
        if (fetched.size() > perPage) {
            MenuLog.warn("符号 '" + symbol + "' 每页返回 " + fetched.size() + " 条，超出容量 "
                    + perPage + "，已截断（fetch 与 perPage 没对齐）");
            fetched = new ArrayList<ItemStack>(fetched.subList(0, perPage));
        }
        pageCache.put(symbol, fetched);
        return fetched;
    }

    /** 兜底填充：没被任何符号占用的格子，以及不可见的符号腾出来的格子。 */
    private void fillEmpty(int index) {
        Items filler = definition.filler();
        if (filler == null) {
            inventory.setItem(index, new ItemStack(Material.AIR));
            return;
        }
        inventory.setItem(index, filler.render(filler.name(), filler.lore()));
    }

    /** 求出这个符号的全部占位符值。单个占位符求值失败不该让整个界面打不开，记空串。 */
    private Map<String, String> placeholders(SymbolState state) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Function<MenuContext, String>> entry : state.placeholders().entrySet()) {
            String value;
            try {
                value = entry.getValue().apply(this);
            } catch (RuntimeException failure) {
                MenuLog.warn("符号 '" + state.symbol() + "' 的占位符 {" + entry.getKey()
                        + "} 求值失败：" + failure);
                value = "";
            }
            values.put(entry.getKey(), value == null ? "" : value);
        }
        return values;
    }

    /** 这个槽位在该符号的槽位表里的下标；不在表里返回 -1。 */
    private static int positionIn(SymbolState state, int index) {
        List<Slot> slots = state.slots();
        for (int position = 0; position < slots.size(); position++) {
            if (slots.get(position).index() == index) {
                return position;
            }
        }
        return -1;
    }

    private static Slot slotAt(SymbolState state, int index) {
        for (Slot slot : state.slots()) {
            if (slot.index() == index) {
                return slot;
            }
        }
        return new Slot(index, index / 9, index % 9);
    }

    // ---------- 派发层要用的读取 ----------

    /** 某一格的渲染结果；空位 / 不可见 / 没渲染过返回 {@code null}。 */
    public Rendered renderedAt(int index) {
        return rendered.get(Integer.valueOf(index));
    }

    public MenuDefinition definition() {
        return definition;
    }

    public MenuSession session() {
        return session;
    }

    /** 容器；给派发层打开用。 */
    public Inventory inventory() {
        return inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** 某个符号当前页的总页数；不是分页符号或总条数未知时返回 -1。 */
    int pageCountOf(SymbolState state) {
        if (state == null || !state.isPaged()) {
            return -1;
        }
        int total = state.pageSource().total(viewer);
        if (total < 0) {
            return -1;
        }
        int perPage = Math.max(1, state.perPage());
        return Math.max(1, (total + perPage - 1) / perPage);
    }

    /**
     * 渲染好的一格：物品、它属于哪个符号与哪一格、以及这一格该跑的点击回调。
     * <p>
     * 点击回调在**渲染时**就解析好（{@code handler}），而不是点击时再按优先级挑：渲染时手里正好有
     * 这一格的符号与槽位，点击时再去反查是重复劳动；而且渲染期已经把「不可见」的格子排除掉了，
     * 所以能拿到 {@code Rendered} 就等于「这一格真的有符号在管事」。
     */
    public static final class Rendered {

        private final ItemStack stack;
        private final SymbolState state;
        private final Slot slot;
        private final ClickHandler handler;

        Rendered(ItemStack stack, SymbolState state, Slot slot, ClickHandler handler) {
            this.stack = stack;
            this.state = state;
            this.slot = slot;
            this.handler = handler;
        }

        public ItemStack stack() {
            return stack;
        }

        public SymbolState state() {
            return state;
        }

        public Slot slot() {
            return slot;
        }

        /** 这一格的点击回调；{@code null} 表示没注册（按 interaction 策略处理）。 */
        public ClickHandler handler() {
            return handler;
        }
    }

    // ---------- MenuControl ----------

    @Override
    public Player viewer() {
        return viewer;
    }

    @Override
    public void close() {
        viewer.closeInventory();
    }

    @Override
    public void refresh() {
        // delayedRefresh(true)：把重画推迟到下一 tick，并把同一 tick 里的多次请求合并成一次。
        // ALLOW_MOVE 下不允许开它（构建期已校验）：玩家可能正在拖一叠物品，下一 tick 重画会把那叠弄丢。
        if (definition.delayedRefresh()) {
            if (refreshScheduled) {
                return;
            }
            refreshScheduled = true;
            MenuScheduler.runNextTick(MenuRegistry.pluginOf(viewer), new Runnable() {
                @Override
                public void run() {
                    refreshScheduled = false;
                    draw();
                }
            });
            return;
        }
        draw();
    }

    @Override
    public MenuSession open(Player other) {
        return Menus.open(definition, other);
    }
    @Override
    public boolean isOpen(Player other) {
        return other != null && MenuSession.isOpenFor(definition.key(), other.getUniqueId());
    }

    @Override
    public void resetSession(Player other) {
        if (other != null) {
            MenuSession.dropFor(definition.key(), other.getUniqueId());
        }
    }

    // ---------- MenuContext（渲染期：没有点击、没有事件） ----------

    @Override
    public ClickType clickType() {
        return null;
    }

    @Override
    public InventoryClickEvent event() {
        return null;
    }

    @Override
    public MenuControl menu() {
        return this;
    }

    @Override
    public List<Slot> symbolSlots() {
        SymbolState state = currentSymbol;
        return state == null ? Collections.<Slot>emptyList() : state.slots();
    }

    @Override
    public Slot slot() {
        return null;
    }

    @Override
    public int page() {
        return session.page();
    }

    @Override
    public void page(int page) {
        session.page(page);
    }

    @Override
    public int perPage() {
        SymbolState state = currentSymbol;
        if (state == null) {
            return 0;
        }
        return state.isPaged() ? state.perPage() : state.slots().size();
    }

    @Override
    public int pageCount() {
        return pageCountOf(currentSymbol);
    }

    @Override
    public int indexInSymbol(Slot slot) {
        SymbolState state = currentSymbol;
        if (state == null || slot == null) {
            return -1;
        }
        return positionIn(state, slot.index());
    }
}
