package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.LayoutParser;
import cn.yvmou.ylib.menu.internal.SymbolState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 声明式构建器：{@code declare(MenuBuilder b)} 里用它把布局里的符号一个个说清楚。
 *
 * <h2>槽位归属的判定顺序</h2>
 * <ol>
 *   <li>被任一符号占据的槽位 → 用该符号的物品；</li>
 *   <li>其余槽位 → 用 {@link #fillEmpty} 的物品；</li>
 *   <li>没设 {@code fillEmpty} → 留空（空气）。</li>
 * </ol>
 * <b>{@code fillEmpty} 的填充物不参与点击派发</b>（没有符号就没有回调），点它等同于点到空槽。
 * 想让边框可点击，就老实用 {@code #} 声明成符号——这也正是「显式符号」与「兜底填充」要并存、
 * 而不是二选一的原因。
 *
 * <h2>为什么符号只能声明一次</h2>
 * {@link #symbol(String)} 调第二次会当场抛。同一个符号对应多个物品没有任何合理解释，
 * 而它的表现（后写的覆盖先写的、或者按调用顺序碰运气）在玩家那里是「这个按钮有时候是另一个东西」。
 */
public final class MenuBuilder {

    /** 符号 → 声明。 */
    private final Map<String, SymbolState> symbols = new LinkedHashMap<String, SymbolState>();

    /** 兜底填充物；没设时为 {@code null}（留空）。 */
    private Items filler;

    /** 事件策略，默认 {@link Interaction#READ_ONLY}。 */
    private Interaction interaction = Interaction.READ_ONLY;

    /** 是否把 {@code refresh()} 推迟到下一 tick。 */
    private boolean delayedRefresh;

    private OpenHandler openHandler;
    private CloseHandler closeHandler;

    /** 由 {@code Menus.register} 创建。 */
    public MenuBuilder() {
    }

    // ---------- 符号 ----------

    /** 单字符符号：布局里可以裸写。 */
    public SymbolBinder symbol(char symbol) {
        return symbol(String.valueOf(symbol));
    }

    /** 多字符符号：布局里必须用反引号包裹。 */
    public SymbolBinder symbol(String name) {
        return symbol(name, name);
    }

    /** 声明符号并显式指定配置键：布局里叫 {@code name}，yml 覆盖时叫 {@code configKey}（单字符平铺符号想要可读键时用）。 */
    public SymbolBinder symbol(char symbol, String configKey) {
        return symbol(String.valueOf(symbol), configKey);
    }

    public SymbolBinder symbol(String name, String configKey) {
        if (name == null || name.isEmpty()) {
            throw new MenuConfigException("符号名不能为空");
        }
        if (name.indexOf('`') >= 0) {
            throw new MenuConfigException("符号名不能含反引号：" + name + "（反引号是布局表的词法边界）");
        }
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new MenuConfigException("符号 '" + name + "' 的配置键不能为空");
        }
        SymbolState existing = symbols.get(name);
        if (existing != null) {
            throw new MenuConfigException("符号 '" + name + "' 被重复声明");
        }
        SymbolState state = new SymbolState(name, configKey);
        symbols.put(name, state);
        return new SymbolBinder(state);
    }

    // ---------- 兜底与策略 ----------

    /**
     * 所有**未被符号占用**的槽位的兜底物品。
     * <p>
     * 不设就留空。空槽不处理在视觉上就是破绽（边框缺一格、菜单看起来没画完），
     * 而「铺一层黑玻璃板」的写法几乎每个菜单都要写一遍，所以它是框架提供的标准件。
     * <p>
     * 只允许调一次：一个菜单只有一种兜底外观。
     */
    public void fillEmpty(Items.Builder item) {
        if (item == null) {
            throw new MenuConfigException("fillEmpty(...) 传了 null");
        }
        if (this.filler != null) {
            throw new MenuConfigException("fillEmpty 只能设置一次");
        }
        this.filler = item.build();
    }

    /** 事件策略；默认 {@link Interaction#READ_ONLY}，显式写出只是为了让读代码的人看到这个决定。 */
    public void interaction(Interaction mode) {
        if (mode == null) {
            throw new MenuConfigException("interaction(...) 传了 null");
        }
        this.interaction = mode;
    }

    /**
     * 把 {@code refresh()} 推迟到下一 tick 执行。
     * <p>
     * 默认 {@code false}：点了按钮就地重画，玩家立刻看到结果。置 {@code true} 时多次
     * {@code refresh()} 会被合并成一次，适合「一次点击引发一批数据变化」的界面。
     * <p>
     * {@link Interaction#ALLOW_MOVE} 下必须保持 {@code false}（校验期会拦）：允许整理时玩家可能正在
     * 拖一叠物品，下一 tick 重画会把光标上的那一叠弄丢。
     */
    public void delayedRefresh(boolean enabled) {
        this.delayedRefresh = enabled;
    }

    /** 打开回调：{@code (viewer, inventory, session)}，每次打开触发一次。 */
    public void onOpen(OpenHandler handler) {
        this.openHandler = handler;
    }

    /** 关闭回调：{@code (viewer, inventory, reason, session)}，每次打开只触发一次。 */
    public void onClose(CloseHandler handler) {
        this.closeHandler = handler;
    }

    // ---------- 构建 ----------

    /**
     * 用模板自己声明的布局构建（{@link MenuTemplate#layout()}）。
     * <p>
     * 配置文件里的 {@code layout} 走另一个重载——布局可以来自 yml，但**由谁来解析仍然只有这一处**，
     * 因此「布局写错」的报错与校验两边完全一致。
     */
    public MenuDefinition build(MenuTemplate template, MenuConfig config) {
        return build(template, config, template.layout());
    }

    /**
     * 解析布局、回填槽位、跑完全部启动期校验，产出不可变的 {@link MenuDefinition}。
     * <p>
     * 由 {@code Menus.register} 调用一次。任何一条校验不过都抛 {@link MenuConfigException}——
     * 菜单的错误全都属于「写错了」，越早报越省事。
     *
     * @param rows 实际使用的布局：模板写的，或配置文件覆盖后的那一份
     */
    public MenuDefinition build(MenuTemplate template, MenuConfig config, String[] rows) {
        LayoutParser.Result layout = LayoutParser.parse(rows, template.columns());

        // 校验 5：单字符符号与多字符符号互为前缀
        LayoutParser.checkPrefixConflict(symbols.keySet());

        // 校验 6：布局里每个符号都在 declare() 中声明过
        for (String symbol : layout.slotsBySymbol().keySet()) {
            if (!symbols.containsKey(symbol)) {
                Slot first = layout.slotsBySymbol().get(symbol).get(0);
                throw new MenuConfigException("符号 '" + symbol + "' 未声明（首次出现在第"
                        + (first.row() + 1) + "行第" + (first.col() + 1) + "列）"
                        + LayoutParser.suggest(symbol, symbols.keySet()));
            }
        }

        // 校验 8 / 9：声明了但没出现在布局里 → 记 warning（便于临时隐藏按钮），不阻断
        List<String> warnings = new ArrayList<String>();
        for (Map.Entry<String, SymbolState> entry : symbols.entrySet()) {
            List<Slot> slots = layout.slotsBySymbol().get(entry.getKey());
            if (slots == null) {
                warnings.add("符号 '" + entry.getKey() + "' 声明了但没有出现在布局中，已忽略");
                continue;
            }
            entry.getValue().slots(slots);
        }

        // 分页符号的每页条数就是它占的格数（SymbolState.perPage 现算），因此没有「两个数对不对得上」这条校验；
        // 唯一要拦的是「声明了分页却没落在布局里」——那会让它一页都铺不出来，上面的 warning 已经报了

        // 校验 15／物品缺失：一个符号既没有静态物品也没有分页数据源 → 界面上那一格永远空着
        for (SymbolState state : symbols.values()) {
            if (state.items() == null && !state.isPaged()) {
                warnings.add("符号 '" + state.symbol() + "' 既没有 of(...) 也没有 pageItems(...)，"
                        + "它的槽位会留空");
            }
        }

        // 校验：ALLOW_MOVE 与 delayedRefresh 不能同时开
        if (interaction == Interaction.ALLOW_MOVE && delayedRefresh) {
            throw new MenuConfigException("ALLOW_MOVE 与 delayedRefresh(true) 不能同时使用："
                    + "允许整理时玩家可能正在拖一叠物品，重画会把光标上的那一叠弄丢");
        }

        return new MenuDefinition(template, layout, symbols, filler, interaction, delayedRefresh,
                openHandler, closeHandler, warnings, config);
    }

    /**
     * 声明过的符号，按声明顺序。
     * <p>
     * {@code build(...)} 之前就要能读到它：配置文件里的 {@code layout} 得先知道「能用哪些符号」
     * 才能校验，而读文件必须排在解析布局之前。
     */
    public Map<String, SymbolState> symbols() {
        return symbols;
    }
}
