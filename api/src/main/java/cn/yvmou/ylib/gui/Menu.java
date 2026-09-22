package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.gui.appearance.MenuAppearance;
import cn.yvmou.ylib.gui.appearance.MenuAppearances;
import cn.yvmou.ylib.gui.appearance.MenuItemAppearance;
import cn.yvmou.ylib.gui.appearance.MenuTexts;
import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * 菜单：**一个界面 = 一份外观 + 一段填充逻辑**，这也是唯一的创建方式。
 * <p>
 * 外观（{@link MenuAppearance}）决定标题、容器大小、布局表与每一格长什么样。两种来源：
 * 代码里直接造（{@code MenuAppearances.create(...)}），或代码定义默认值、文件交给 YLib 管理
 * （{@code MenuAppearances.file(...)}——文件不存在按默认值生成，存在则以文件为准）。
 * <p>
 * 填充只有两种写法，复杂界面继承本类实现 {@link #fill()}，简单界面不建类、给一段 lambda：
 * <pre>{@code
 * Menu.of(viewer, messages, appearance, null, menu -> {
 *     menu.item("buy", Placeholders.vars("price", 100), click -> buy(click.player()));
 *     menu.background();
 * }).open();
 * }</pre>
 * 填充能用的方法（{@code item} / {@code itemAt} / {@code hint} / {@code background} / {@code textOf} /
 * {@code loreOf} / {@code pagedList} …）都是 public——{@code Menu.of} 的 lambda 与继承写法用的是同一套。
 * <p>
 * 本类兜住的几件事：
 * <ul>
 *   <li><b>尺寸与布局顺序</b>：容器大小由布局行数算（{@code SlotLayout.sizeOf}），{@link #build()} 是 final
 *       （先 {@code layout(...)} 再 {@link #fill()}），写不出「还没声明布局就按名字取槽位」这种顺序错误；</li>
 *   <li><b>归属判定</b>用 {@link MenuHolder} 对象身份而不是标题（标题会随语言变、也可能与别的插件撞名）；</li>
 *   <li><b>内容在第一次 {@link #open()} 时构建</b>，子类不必（也不能）在构造里构建——那时子类字段还没赋值；</li>
 *   <li><b>缺配置的两种表现分开处理</b>：布局里没这个名字 → 跳过（管理员删掉一个入口是合法操作）；
 *       布局里有、外观里没有 → 当场抛并指明是哪个文件的哪个键；</li>
 *   <li><b>契约对账</b>：每次构建后自动比对「模板要的占位符」与「代码给的占位符」，不一致就记日志
 *       （严格模式下抛），报告见 {@code MenuAppearances}。</li>
 * </ul>
 * 注册点击分发见 {@link MenuListener#init(org.bukkit.plugin.Plugin)}（不注册则界面能开、点了没反应）。
 * <p>
 * 泛型参数 {@code C} 是**应用上下文**（子系统装配结果这类东西）：菜单几乎都要用它取数据，
 * 而它与界面无关，不必逼每个子类自己存一份字段；用 {@code Menu.of} 时在 lambda 外层捕获即可，
 * 所以 {@link #context()} 只留给子类。
 */
public abstract class Menu<C> {

    /** 打开界面的玩家（本菜单只为这一个玩家构建）。 */
    private final Player viewer;

    /** 语言服务；用 {@code null} 表示不做翻译，模板按字面量处理。 */
    private final MessageService messages;

    /** 容器大小，已规范为 9 的倍数。 */
    private final int size;

    /** 槽位 → 菜单项。以它而不是 inventory 内容为准：取出来的 ItemStack 已被 Bukkit 复制过。 */
    private final Map<Integer, MenuItem> items = new HashMap<Integer, MenuItem>();

    /** 归属标记，必须在 inventory 之前初始化（createInventory 需要它）。 */
    private final MenuHolder holder;

    private final Inventory inventory;

    /** 这个菜单的外观（标题 / 布局 / 物品表 / 数据文案）：一个菜单一份，因此不必拿字符串路径去查表。 */
    private final MenuAppearance appearance;

    /** 应用上下文：界面无关，只是免得每个子类自己存一份。 */
    private final C context;

    /** {@link #refresh()} 的重入保护。 */
    private boolean rebuilding;

    /** 是否已经构建过内容（第一次 {@link #open()} 才构建）。 */
    private boolean built;

    /** 打开、关闭时的回调；没设过则为 {@code null}。 */
    private Consumer<Player> openCallback;
    private Consumer<Player> closeCallback;

    /**
     * 本次打开是否已经通知过关闭。
     * <p>
     * 玩家客户端强制关闭界面时，原版会发两次关闭事件——不设这道闸门，回调就会跑两次
     * （常见表现是「返回上一级」被连点两下）。每次 {@link #open()} 重置。
     */
    private boolean closeNotified;

    /** 本菜单是否锁主手；{@code null} 表示跟随 {@link #handLockedByDefault()} 的全局默认。 */
    private Boolean handLocked;

    /** 全局默认：菜单打开时是否锁主手。默认**关**——库默认改玩家的手感是要挨骂的。 */
    private static volatile boolean handLockedByDefault = false;

    /** 由 {@link #layout(String...)} 声明的布局；没声明时为 {@code null}（那时只能按数字下标摆位）。 */
    private SlotLayout layout;

    /** 契约账本：本次构建里，每个用到的语言键**要哪些**占位符名字（模板扫出来的）。 */
    private final Map<String, Set<String>> contractRequired = new LinkedHashMap<String, Set<String>>();

    /** 契约账本：本次构建里，每个用到的语言键**给了哪些**占位符名字。 */
    private final Map<String, Set<String>> contractProvided = new LinkedHashMap<String, Set<String>>();

    /**
     * @param appearance 这个菜单的外观；{@code null} 当场抛（没有外观就没有界面可开）
     * @param context    应用上下文，子类用 {@link #context()} 取
     */
    protected Menu(Player viewer, MessageService messages, MenuAppearance appearance, C context) {
        this(viewer, messages, appearance, context, Placeholders.empty());
    }

    /**
     * 标题里要填占位符时用这个（如任务详情的 {@code {quest}}）。
     * <p>
     * 标题在构造里就定稿（容器的名字建好就不能改），所以取值必须发生在 {@code super(...)} 的参数里，
     * 那时还读不到子类字段——需要任务名这类数据时，请在 {@code super(...)} 里就地取出来传进来。
     */
    protected Menu(Player viewer, MessageService messages, MenuAppearance appearance, C context,
                   Map<String, String> titleVars) {
        if (appearance == null) {
            throw new IllegalArgumentException("菜单外观是 null：没有外观就没有界面可开");
        }
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.messages = messages;
        this.appearance = appearance;
        this.context = context;
        this.size = normalizeSize(SlotLayout.sizeOf(appearance.layout()));
        this.holder = new MenuHolder();
        // holder 先于 inventory 赋值：createInventory 需要 holder，而 holder.getInventory() 读的正是下面这个字段
        this.inventory = Bukkit.createInventory(holder, this.size,
                MenuTexts.text(messages, viewer, appearance.titleKey(), appearance.title(), titleVars));
    }

    /**
     * 不建类，给一段填充逻辑即可：适合只有两三个按钮的简单界面。
     * <p>
     * 填充逻辑在第一次打开（以及每次 {@link #refresh()}）时执行，因此要按当前数据重新算一遍，
     * 与 {@link #fill()} 的约束完全一样；lambda 里能用的方法与继承写法是同一套（都是 public）。
     */
    public static <C> Menu<C> of(Player viewer, MessageService messages, MenuAppearance appearance, C context,
                                 final Consumer<Menu<C>> filler) {
        return new Menu<C>(viewer, messages, appearance, context) {
            @Override
            protected void fill() {
                filler.accept(this);
            }
        };
    }

    // ---------- 基本访问 ----------

    /** 打开界面的玩家。 */
    public final Player viewer() {
        return viewer;
    }

    /** 语言服务；构造时没传则为 {@code null}。 */
    protected final MessageService messages() {
        return messages;
    }

    /** 容器大小（槽位数），由外观的布局行数算出来。 */
    public final int size() {
        return size;
    }

    /** 这个菜单的外观。 */
    public final MenuAppearance appearance() {
        return appearance;
    }

    /** 应用上下文（子系统装配结果这类东西）。 */
    protected final C context() {
        return context;
    }

    // ---------- 子类接口 ----------

    /**
     * 按当前数据摆物品。会随 {@link #refresh()} 反复调用，因此每次都要重新算一遍，不能依赖上一次的结果。
     * <p>
     * 布局已经由基类声明好（{@link #build()} 是 final 的），这里直接 {@code item(...)} / {@code fill(...)} 即可。
     */
    protected abstract void fill();

    /** 装配过程：先按外观声明布局，再交给子类填充，最后对一次契约账。 */
    private void build() {
        layout(appearance.layout());
        contractRequired.clear();
        contractProvided.clear();
        fill();
        auditContract();
    }

    // ---------- 生命周期 ----------

    /**
     * 打开界面：第一次打开时才构建内容（此后 {@link #refresh()} 随叫随到）。
     * <p>
     * 因此子类<b>不必</b>在构造里调 {@link #refresh()}——基类构造期间子类字段还没赋值，那时候构建读到的是 null。
     */
    public final void open() {
        if (!built) {
            refresh();
        }
        // 重新打开是一次新的生命周期：上一轮的关闭通知得作废，否则这次关闭回调不会触发
        closeNotified = false;
        viewer.openInventory(inventory);
    }

    /** 关掉这个界面（玩家按 ESC 也是同样效果）。 */
    public final void close() {
        viewer.closeInventory();
    }

    /**
     * 清空后重跑填充逻辑，用于操作后即时刷新界面。
     * <p>
     * 正在重建时再次调用会直接返回：动作里刷新、{@code fill()} 里又刷新这类写法
     * 若没有这道闸门就是无限递归。
     */
    public final void refresh() {
        if (rebuilding) {
            return;
        }
        rebuilding = true;
        built = true;
        try {
            items.clear();
            inventory.clear();
            build();
        } finally {
            rebuilding = false;
        }
    }

    /**
     * 打开时触发（每次 {@link #open()} 都会）：适合「打开时记一笔」「看一眼当前数据」这类事。
     * <p>
     * 与 {@link #fill()} 的分工：{@code fill()} 负责把界面画出来（可能被反复调用），
     * 这里只表示「这个玩家看到了这个界面」，因此不会因为刷新内容而重复触发。
     */
    public final void onOpen(Consumer<Player> callback) {
        this.openCallback = callback;
    }

    /**
     * 关闭时触发，**每次打开只触发一次**。
     * <p>
     * 玩家按 ESC、被别的插件关掉、或这个菜单被另一个界面顶掉，都会走这里。
     * 库负责把「客户端强制关闭会发两次事件」压成一次，因此回调里可以放心做计费、发奖这类不能重复的动作。
     */
    public final void onClose(Consumer<Player> callback) {
        this.closeCallback = callback;
    }

    // ---------- 主手锁定 ----------

    /**
     * 全局默认：菜单打开时是否锁住主手。宿主在 {@link MenuListener#init(org.bukkit.plugin.Plugin)} 旁边
     * 写一句 {@code Menu.handLockedByDefault(true)} 就对本插件所有菜单生效；个别菜单不同意，
     * 用实例的 {@link #handLocked(boolean)} 单独关掉。
     */
    public static void handLockedByDefault(boolean locked) {
        handLockedByDefault = locked;
    }

    /** 当前全局默认。 */
    public static boolean handLockedByDefault() {
        return handLockedByDefault;
    }

    /**
     * 这个菜单打开时是否锁住主手（阻止丢弃物品、换手、切快捷栏）。
     * <p>
     * 不设就是跟随 {@link #handLockedByDefault()} 的全局默认（默认**不锁**：库不该默认改玩家的手感）。
     * 菜单里有「让玩家把物品拿进来」这类交互时，通常该把它关掉。
     */
    public final void handLocked(boolean locked) {
        this.handLocked = locked;
    }

    // ---------- 布局 ----------

    /**
     * 声明界面布局：把界面画成一张文本图（见 {@link SlotLayout}）。
     * <p>
     * 只有 {@link #build()} 调它——布局来自外观，所以外观驱动的菜单不需要（也不该）自己声明布局。
     */
    private void layout(String... rows) {
        this.layout = SlotLayout.parse(size, rows);
    }

    /** 这个名字占的格子下标，按阅读顺序；没声明布局、或名字不在表里都抛（写错必须当场炸）。 */
    public final List<Integer> slots(String name) {
        if (layout == null) {
            throw new IllegalStateException("还没声明布局就按名字取槽位「" + name + "」：先在 fill() 里调用 layout(...)");
        }
        return layout.slots(name);
    }

    /** 布局里有没有这个名字（可选摆位用：可能被关掉的分页按钮、可选标签，布局里没写就别摆）。 */
    public final boolean declared(String name) {
        return layout != null && layout.names().contains(name);
    }

    // ---------- 摆物品（低级：数据驱动的物品） ----------

    /**
     * 在指定槽位放一个菜单项。
     * <p>
     * 越界或 {@code null} 会被静默忽略：界面数据可能来自配置与数据库，
     * 不该因为一条脏数据（目标数超出预留区域）就抛异常把整个界面打断。
     */
    public final void set(int slot, MenuItem item) {
        if (item == null || slot < 0 || slot >= size) {
            return;
        }
        items.put(slot, item);
        inventory.setItem(slot, item.icon());
    }

    /** 静态槽位：这个名字占的每一格都放同一个物品（单格就是常规用法）；名字写错会抛。 */
    public final void set(String name, MenuItem item) {
        for (int slot : slots(name)) {
            set(slot, item);
        }
    }

    /**
     * 动态槽位：把一串物品按顺序填进这个名字占的格子（任务列表、候选列表这种）。
     * 物品比格子多时多的不显示，物品比格子少时剩下的格子留空。
     */
    public final void fill(String name, List<MenuItem> filled) {
        if (layout == null) {
            throw new IllegalStateException("还没声明布局就按名字填槽位「" + name + "」：先在 fill() 里调用 layout(...)");
        }
        List<Integer> slots = layout.take(name, filled == null ? 0 : filled.size());
        for (int index = 0; index < slots.size(); index++) {
            set(slots.get(index), filled.get(index));
        }
    }

    /** 用同一个菜单项填满尚未占用的空位（背景板、禁用态按钮的铺底）。 */
    public final void fill(MenuItem item) {
        for (int slot = 0; slot < size; slot++) {
            if (!items.containsKey(slot)) {
                set(slot, item);
            }
        }
    }

    // ---------- 摆物品（外观：材质与文案来自配置） ----------

    /** 摆一个配置化物品：材质、名称与 lore 都来自外观（可被语言文件覆盖），只有动作在这里给。 */
    public final void item(String name, Consumer<MenuItem.ClickContext> action) {
        item(name, Placeholders.empty(), action);
    }

    /** 只展示、不响应点击的配置化物品。 */
    public final void item(String name) {
        item(name, Placeholders.empty(), null);
    }

    /**
     * 摆一个配置化物品。
     *
     * @param name   布局里的槽位名，同时是物品表的键与语言键后缀
     * @param vars   名称与 lore 里命名占位符的取值；没有占位符时传 {@code Placeholders.empty()}
     * @param action 点击动作；只展示不响应时传 {@code null}
     */
    public final void item(String name, Map<String, String> vars, Consumer<MenuItem.ClickContext> action) {
        if (!declared(name)) {
            return;
        }
        MenuItemAppearance config = require(name);
        String key = appearance.itemKey(name);
        set(name, buildItem(config, key, vars, action));
    }

    /**
     * 摆在指定槽位上的配置化物品：给「位置由代码算、不在布局表里」的物品用（空列表提示这类）。
     * 名字仍要能在物品表里找到，因此文案与语言键的规则与 {@link #item(String, Map, Consumer)} 完全一致。
     */
    public final void itemAt(int slot, String name, Map<String, String> vars, Consumer<MenuItem.ClickContext> action) {
        MenuItemAppearance config = require(name);
        set(slot, buildItem(config, appearance.itemKey(name), vars, action));
    }

    /** 简化写法：不填占位符、不响应点击。 */
    public final void itemAt(int slot, String name) {
        itemAt(slot, name, Placeholders.empty(), null);
    }

    /** 简化写法：不填占位符。 */
    public final void itemAt(int slot, String name, Consumer<MenuItem.ClickContext> action) {
        itemAt(slot, name, Placeholders.empty(), action);
    }

    /**
     * 摆在布局名上、能**接收玩家拖入物品**的配置化物品：外观与文案规则同 {@link #item}，
     * 多一个收到物品时的回调（收到的是克隆，原物品不消耗、回到玩家手里）。
     * 空手点击仍走 {@code action}——「左键改输入、右键恢复默认」这类手势都在动作里分。
     */
    public final void itemAccepting(String name, Consumer<ItemStack> onItem, Consumer<MenuItem.ClickContext> action) {
        itemAccepting(name, Placeholders.empty(), onItem, action);
    }

    /** 能接收玩家拖入物品的配置化物品（带占位符）。 */
    public final void itemAccepting(String name, Map<String, String> vars,
                                       Consumer<ItemStack> onItem, Consumer<MenuItem.ClickContext> action) {
        if (!declared(name)) {
            return;
        }
        MenuItemAppearance config = require(name);
        String key = appearance.itemKey(name);
        record(key, itemNames(config, key), vars);
        set(name, MenuItem.acceptItem(
                MenuItem.material(config.material()),
                MenuTexts.text(messages, viewer, key + ".name", config.name(), vars),
                lore -> lore.addAll(MenuTexts.lines(messages, viewer, key + ".lore", config.lore(), vars)),
                onItem, action));
    }

    /**
     * 一个区域空着时的提示：在它的首格摆一个只展示的物品（「还没有任务」这类）。
     * 区域里有东西时不要调用它——那时该摆的是条目本身。
     */
    public final void hint(String area, String name) {
        hint(area, name, Placeholders.empty());
    }

    /** 一个区域空着时的提示（带占位符，如「还没有{type}任务」）。 */
    public final void hint(String area, String name, Map<String, String> vars) {
        itemAt(slots(area).get(0), name, vars, null);
    }

    /**
     * 铺背景板：填满没被按钮占用的格子。外观里没配 {@code items.filler} 时退回库自带的灰色玻璃板——
     * 背景板缺配置不该让界面打不开，它本来就是个装饰。
     */
    public final void background() {
        MenuItemAppearance config = appearance.items().get(MenuAppearance.FILLER);
        fill(config == null
                ? MenuItem.filler()
                : buildItem(config, appearance.itemKey(MenuAppearance.FILLER), Placeholders.empty(), null));
    }

    // ---------- 文案 ----------

    /**
     * 取一条文案：语言键 {@code gui.<逻辑菜单名>.<后缀>} 优先，否则用外观里 {@link MenuAppearance#texts()}
     * 那一项的模板（多行按换行拼）。数据驱动的一行（状态词这类）用它。
     * <p>
     * 外观里没有这个后缀时当场抛——和物品一样：摆了就得给它一份外观，否则界面上是一句没有出处的文案。
     */
    public final String textOf(String suffix, Map<String, String> vars) {
        return textOf(suffix, null, vars);
    }

    /**
     * 带**代码兜底**的取法：外观里没有这个后缀时用 {@code fallback}。
     * <p>
     * 给「本来就不该进配置」的文案用——排序维度的显示名这种：比较逻辑写在代码里，名字自然也在代码里，
     * 逼它进配置只会多一份要和枚举对齐的清单。外观里写了就以外观的为准，语言文件里写了以语言文件为准。
     */
    public final String textOf(String suffix, String fallback, Map<String, String> vars) {
        List<String> templates = appearance.texts().get(suffix);
        String template = templates == null ? fallback : String.join("\n", templates);
        if (template == null) {
            throw new IllegalStateException(appearance.fileName() + " 的 " + appearance.key(suffix)
                    + " 没有定义：代码里取了这一处文案，就得在 texts() 里给它一份模板（或传一个兜底）");
        }
        String key = appearance.key(suffix);
        record(key, requiredNames(key, template, null), vars);
        return MenuTexts.text(messages, viewer, key, template, vars);
    }

    /** 不填占位符的取法。 */
    public final String textOf(String suffix) {
        return textOf(suffix, null, Placeholders.empty());
    }

    /** 不填占位符的取法（带代码兜底）。 */
    public final String textOf(String suffix, String fallback) {
        return textOf(suffix, fallback, Placeholders.empty());
    }

    /**
     * 取一组文案（lore）：语言键 {@code gui.<逻辑菜单名>.<后缀>} 优先，否则用外观里的模板列表。
     * 给**数据驱动的物品**用（任务列表的条目这种）——它们的材质与名称来自数据本身，能配的只有 lore。
     */
    public final List<String> loreOf(String suffix, Map<String, String> vars) {
        return loreOf(suffix, null, vars);
    }

    /** 带代码兜底的取法（见 {@link #textOf(String, String, Map)}）。 */
    public final List<String> loreOf(String suffix, List<String> fallback, Map<String, String> vars) {
        List<String> templates = appearance.texts().get(suffix);
        if (templates == null) {
            templates = fallback;
        }
        if (templates == null) {
            throw new IllegalStateException(appearance.fileName() + " 的 " + appearance.key(suffix)
                    + " 没有定义：代码里取了这一处文案，就得在 texts() 里给它一份模板（或传一个兜底）");
        }
        String key = appearance.key(suffix);
        record(key, requiredNames(key, null, templates), vars);
        return MenuTexts.lines(messages, viewer, key, templates, vars);
    }

    /** 不填占位符的取法。 */
    public final List<String> loreOf(String suffix) {
        return loreOf(suffix, null, Placeholders.empty());
    }

    /**
     * 渲染语言键文本（占位符按 {@code {0}}、{@code {1}}… 顺序替换）。
     * <p>
     * 接了语言服务时按玩家客户端语言解析（与聊天提示保持同一语言）；没接时键本身就是要显示的字。
     */
    public final String text(String key, Object... args) {
        if (messages == null) {
            return literal(key, args);
        }
        return cn.yvmou.ylib.text.TextRenderer.render(messages.raw(viewer, key, args));
    }

    /**
     * 有语言键就用它，没有就用自带文案——库自带的默认文案（翻页按钮这类）用它：
     * 宿主定义了自己的键就自动生效，没定义也不会在界面上显示「Missing message: ...」。
     */
    public final String textOr(String key, String fallback, Object... args) {
        return messages != null && messages.has(key) ? text(key, args) : literal(fallback, args);
    }

    // ---------- 分页 ----------

    /**
     * 按页填一个区域：页码先夹紧（数据变少后旧页码会越界），再把这一页的条目交给 {@code renderer} 生成。
     * <p>
     * 每页格数就是该区域在布局表里的格数，因此调用方不必自己算「一页能放几条」——
     * 这几个数一旦对不上，表现是某一页少一条或多一条，在游戏里极难发现。
     *
     * @param page     想显示的第几页（0 起，越界会夹回合法范围）
     * @param area     布局表里的区域名（它占几格就一页放几条）
     * @param elements 全部条目（不受页码影响的完整列表）
     * @param renderer 把一条数据变成一个菜单项
     * @return 夹紧后的翻页状态，翻页按钮据此决定显示与动作
     */
    public final <T> Pager pagedList(int page, String area, List<T> elements, Function<T, MenuItem> renderer) {
        int pageSize = slots(area).size();
        Pager pager = Pager.of(page, elements == null ? 0 : elements.size(), pageSize);
        List<T> slice = Pager.slice(elements, pager.page(), pageSize);
        List<MenuItem> rendered = new ArrayList<MenuItem>(slice.size());
        for (T each : slice) {
            rendered.add(renderer.apply(each));
        }
        fill(area, rendered);
        return pager;
    }

    /**
     * 按页填一个区域，并在**一条数据都没有**时摆上提示物品。
     * <p>
     * 「列表区空了要有人说话」几乎是每个列表界面的固定需求，因此合成一次调用：
     * 少了它，每个菜单都要自己写一遍「先铺条目、再判断是不是空、空了就在首格放提示」。
     */
    public final <T> Pager pagedList(int page, String area, List<T> elements,
                                        Function<T, MenuItem> renderer, String emptyName) {
        Pager pager = pagedList(page, area, elements, renderer);
        if (elements == null || elements.isEmpty()) {
            hint(area, emptyName);
        }
        return pager;
    }

    // ---------- 契约对账 ----------

    /**
     * 记一笔「这一处用了哪些占位符名字、模板要哪些名字」。
     * <p>
     * 账本是**这次 {@link #fill()} 跑出来的**，所以它不会和代码漂移——这也是不给菜单加「声明清单」的原因。
     */
    private void record(String key, Set<String> required, Map<String, String> vars) {
        Set<String> wanted = contractRequired.get(key);
        if (wanted == null) {
            wanted = new LinkedHashSet<String>();
            contractRequired.put(key, wanted);
        }
        wanted.addAll(required);

        Set<String> given = contractProvided.get(key);
        if (given == null) {
            given = new LinkedHashSet<String>();
            contractProvided.put(key, given);
        }
        if (vars != null) {
            given.addAll(vars.keySet());
        }
    }

    /** 构建末尾对账：模板要的没给 → 日志（严格模式下抛）；给了但没人要 → 只记进报告。 */
    private void auditContract() {
        if (MenuAppearances.checkContract()) {
            for (Map.Entry<String, Set<String>> entry : contractRequired.entrySet()) {
                String key = entry.getKey();
                Set<String> required = entry.getValue();
                Set<String> provided = contractProvided.get(key);
                Set<String> missing = new LinkedHashSet<String>(required);
                missing.removeAll(provided == null ? Collections.<String>emptySet() : provided);
                if (missing.isEmpty()) {
                    continue;
                }
                String message = appearance.fileName() + " 的 " + key + " 用了 "
                        + describe(missing) + "，但代码没提供它（代码这次给的是："
                        + describe(provided) + "）";
                if (MenuAppearances.strictContract()) {
                    throw new IllegalStateException("菜单契约: " + message);
                }
                MenuAppearances.warn("菜单契约: " + message);
            }
        }
        // 无论是否开启检查都要记：报告靠它，否则「给了但没用上 / 配了但从没被摆过」永远查不到
        MenuAppearances.record(appearance, contractRequired, contractProvided);
    }

    private static String describe(Set<String> names) {
        return names == null || names.isEmpty() ? "[]" : names.toString();
    }

    /** 某一格物品的名称 + lore 模板里要的名字（含语言文件覆盖）。 */
    private Set<String> itemNames(MenuItemAppearance config, String key) {
        Set<String> names = new LinkedHashSet<String>();
        names.addAll(requiredNames(key + ".name", config.name()));
        names.addAll(requiredNames(key + ".lore", null, config.lore()));
        return names;
    }

    /**
     * 模板 + 语言文件覆盖一起扫：语言文件里写了 {@code {count}} 而配置里没写，也算用到了，
     * 否则「代码给的值没人用」会误报。
     */
    private Set<String> requiredNames(String languageKey, String single) {
        return requiredNames(languageKey, single, null);
    }

    private Set<String> requiredNames(String languageKey, String single, List<String> templates) {
        Set<String> names = new LinkedHashSet<String>();
        if (single != null) {
            names.addAll(Placeholders.names(single));
        }
        if (templates != null) {
            names.addAll(Placeholders.names(templates));
        }
        names.addAll(Placeholders.names(MenuTexts.languageValue(messages, viewer, languageKey)));
        return names;
    }

    /** 外观里缺这一格就是配置错：界面上会空一格（且没有别的提示），抛出来并指明是哪个文件哪个键。 */
    private MenuItemAppearance require(String name) {
        MenuItemAppearance config = appearance.items().get(name);
        if (config == null) {
            throw new IllegalStateException(appearance.fileName() + " 的 items." + name
                    + " 没有定义：界面里摆了这一格，就得给它一份外观（材质 / 名称 / lore）");
        }
        return config;
    }

    /** 外观 → 菜单项。名称与 lore 各查一次语言键（{@code …items.<名字>.name} / {@code .lore}）。 */
    private MenuItem buildItem(MenuItemAppearance config, String key, Map<String, String> vars,
                               Consumer<MenuItem.ClickContext> action) {
        record(key, itemNames(config, key), vars);
        return MenuItem.of(
                MenuItem.material(config.material()),
                MenuTexts.text(messages, viewer, key + ".name", config.name(), vars),
                lore -> lore.addAll(MenuTexts.lines(messages, viewer, key + ".lore", config.lore(), vars)),
                action);
    }

    // ---------- 供监听器使用（包内可见，不对外暴露） ----------

    /** 取槽位上的菜单项，没有则返回 {@code null}。 */
    final MenuItem itemAt(int slot) {
        return items.get(slot);
    }

    /** 本菜单实际生效的主手锁定状态（{@link #handLocked(boolean)} 优先，否则取全局默认）。 */
    final boolean lockedHand() {
        return handLocked == null ? Boolean.valueOf(handLockedByDefault) : handLocked;
    }

    /** 打开通知；没设过回调就什么都不做。 */
    final void notifyOpen(Player player) {
        if (openCallback != null) {
            openCallback.accept(player);
        }
    }

    /** 关闭通知；每次打开只放行一次（见 {@link #closeNotified}）。 */
    final void notifyClose(Player player) {
        if (closeNotified) {
            return;
        }
        closeNotified = true;
        if (closeCallback != null) {
            closeCallback.accept(player);
        }
    }

    // ---------- 内部实现 ----------

    /** 渲染一段成品文本并替换占位符（不走语言文件）。 */
    private static String literal(String template, Object... args) {
        return cn.yvmou.ylib.text.TextRenderer.render(format(template, args));
    }

    /** 没接语言服务时的占位符替换；未提供的占位符静默移除。 */
    private static String format(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }
        String result = template;
        for (int index = 0; index < args.length; index++) {
            result = result.replace("{" + index + "}", String.valueOf(args[index]));
        }
        return result;
    }

    /**
     * 规范化容器大小。
     * <p>
     * {@code Bukkit.createInventory} 对非 9 倍数的大小会直接抛 {@code IllegalArgumentException}，
     * 与其让调用方在构造界面时炸掉，不如在这里夹紧到合法区间。
     */
    private static int normalizeSize(int size) {
        if (size < 9) {
            return 9;
        }
        if (size > 54) {
            return 54;
        }
        return size - (size % 9);
    }

    /** 归属标记：做成非静态内部类，{@link #getInventory()} 直接返回外层字段，因此不存在读到 {@code null} 的窗口。 */
    public final class MenuHolder implements InventoryHolder {

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        /** 容器归属的菜单；监听器据此派发点击。 */
        Menu<?> menu() {
            return Menu.this;
        }
    }
}
