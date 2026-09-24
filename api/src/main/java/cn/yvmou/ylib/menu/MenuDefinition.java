package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.LayoutParser;
import cn.yvmou.ylib.menu.internal.SymbolState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个菜单的**不可变静态定义**：注册期产出一次，之后所有玩家、所有次打开共用同一份。
 * <p>
 * 「静态」是这个词的重点：这里只有布局、符号与「怎么做」（占位符函数、点击回调），
 * 没有任何「这次打开的玩家是谁」。每次打开的全部可变状态都住在 {@link MenuRender} / {@link MenuSession} 里。
 * <p>
 * 不可变带来两个直接好处：渲染是纯读操作（不会因为某个玩家的操作污染其他玩家的界面），
 * 而且占位符必然在渲染期求值——想「构建期冻结一个数」在这一层根本无处可存。
 */
public final class MenuDefinition {

    private final MenuTemplate template;
    private final MenuConfig config;
    private final LayoutParser.Result layout;
    private final Map<String, SymbolState> symbols;
    private final Items filler;
    private final Interaction interaction;
    private final boolean delayedRefresh;
    private final OpenHandler openHandler;
    private final CloseHandler closeHandler;
    private final List<String> warnings;

    MenuDefinition(MenuTemplate template, LayoutParser.Result layout, Map<String, SymbolState> symbols,
                   Items filler, Interaction interaction, boolean delayedRefresh,
                   OpenHandler openHandler, CloseHandler closeHandler, List<String> warnings, MenuConfig config) {
        this.template = template;
        this.config = config;
        this.layout = layout;
        this.symbols = Collections.unmodifiableMap(new LinkedHashMap<String, SymbolState>(symbols));
        this.filler = filler;
        this.interaction = interaction;
        this.delayedRefresh = delayedRefresh;
        this.openHandler = openHandler;
        this.closeHandler = closeHandler;
        this.warnings = Collections.unmodifiableList(new java.util.ArrayList<String>(warnings));
    }

    // ---------- 身份 ----------

    /**
     * 模板类名：作为 session 的表键。
     * <p>
     * 用类名而不是 {@code toString()} 或对象身份：同一个菜单可能被 {@code new} 出多个实例
     * （模板可以持有 Service，装配方式不止一种），但「玩家在这个菜单上的页码」只该有一份。
     */
    public String key() {
        return template.getClass().getName();
    }

    public MenuTemplate template() {
        return template;
    }

    /** 模板上的 {@code @MenuConfig}；没标时为 {@code null}。 */
    public MenuConfig config() {
        return config;
    }

    // ---------- 布局 ----------

    /** 容器大小（槽位数）。 */
    public int size() {
        return layout.size();
    }

    public int columns() {
        return layout.columns();
    }

    /** 某个槽位上的符号；空位返回 {@code null}。 */
    public String symbolAt(int slot) {
        return layout.symbolAt(slot);
    }

    /** 某个符号占据的槽位；没这个符号返回空表。 */
    public List<Slot> slotsOf(String symbol) {
        List<Slot> slots = layout.slotsBySymbol().get(symbol);
        return slots == null ? Collections.<Slot>emptyList() : slots;
    }

    /** 某个符号的声明；没声明过返回 {@code null}。 */
    public SymbolState stateOf(String symbol) {
        return symbols.get(symbol);
    }

    /** 按 yml 的键（{@code item("...")} 里那个）找符号声明；找不到返回 {@code null}。 */
    public SymbolState stateByConfigKey(String configKey) {
        if (configKey == null) {
            return null;
        }
        for (SymbolState state : symbols.values()) {
            if (configKey.equals(state.configKey())) {
                return state;
            }
        }
        return null;
    }

    /** 全部符号声明，按声明的先后顺序。 */
    public Map<String, SymbolState> symbols() {
        return symbols;
    }

    // ---------- 策略 ----------

    public Items filler() {
        return filler;
    }

    public Interaction interaction() {
        return interaction;
    }

    public boolean delayedRefresh() {
        return delayedRefresh;
    }

    public OpenHandler openHandler() {
        return openHandler;
    }

    public CloseHandler closeHandler() {
        return closeHandler;
    }

    /** 注册期产生的非致命提醒（声明了却没进布局、符号没给物品……），由注册处逐条打日志。 */
    public List<String> warnings() {
        return warnings;
    }

    // ---------- 渲染辅助 ----------

    /** 这个槽位是不是「兜底填充物」：没有任何符号占据它。 */
    public boolean isFillerSlot(int slot) {
        return layout.symbolAt(slot) == null;
    }

    /** 兜底填充物的一个成品；没设 {@code fillEmpty} 时为 {@link Material#AIR}。 */
    public ItemStack fillerStack() {
        if (filler == null) {
            return new ItemStack(Material.AIR);
        }
        return filler.render(filler.name(), filler.lore());
    }
}
