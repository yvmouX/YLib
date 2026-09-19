package cn.yvmou.ylib.gui;

import cn.yvmou.ylib.message.MessageService;
import cn.yvmou.ylib.text.TextRenderer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 箱子菜单框架：建容器、登记槽位、刷新重建、标记归属，摆什么物品交给子类在 {@link #build()} 里决定。
 * <p>
 * 用法：继承本类 → 在 {@link #build()} 里 {@link #layout(String...)} 画出界面、按名字摆物品 →
 * {@link #open()} 打开。三个容易踩的点都在框架里兜住了：
 * <ul>
 *   <li>内容在第一次 {@link #open()} 时构建，子类<b>不必也不能</b>在构造里构建（那时子类字段还没赋值）；</li>
 *   <li>归属判定用 {@link MenuHolder} 对象身份而不是标题（标题会随语言变、也可能与别的插件撞名）；</li>
 *   <li>{@link #set(int, MenuItem)} 对越界与 {@code null} 静默忽略，脏数据不该把界面打断。</li>
 * </ul>
 * 注册点击分发见 {@link MenuListener#init(org.bukkit.plugin.Plugin)}（不注册则界面能开、点了没反应）。
 */
public abstract class Menu {

    /** 打开界面的玩家（本菜单只为这一个玩家构建）。 */
    private final Player viewer;

    /** 语言服务；用 {@link #Menu(Player, int, String)} 构造时为 {@code null}（那时文案按成品文本处理）。 */
    private final MessageService messages;

    /** 容器大小，已规范为 9 的倍数。 */
    private final int size;

    /** 槽位 → 菜单项。以它而不是 inventory 内容为准：取出来的 ItemStack 已被 Bukkit 复制过。 */
    private final Map<Integer, MenuItem> items = new HashMap<Integer, MenuItem>();

    /** 归属标记，必须在 inventory 之前初始化（createInventory 需要它）。 */
    private final MenuHolder holder;

    private final Inventory inventory;

    /** {@link #refresh()} 的重入保护。 */
    private boolean rebuilding;

    /** 是否已经构建过内容（第一次 {@link #open()} 才构建）。 */
    private boolean built;

    /** 由 {@link #layout(String...)} 声明的布局；没声明时为 {@code null}（那时只能按数字下标摆位）。 */
    private SlotLayout layout;

    /** 纯文本标题：不接语言服务，适合只做一种语言的场景。 */
    protected Menu(Player viewer, int size, String title) {
        this(viewer, null, size, title);
    }

    /**
     * 标题走「语言键优先，取不到就当字面量」：有同名语言键就按玩家语言解析，没有就把传入的字符串本身渲染出来
     * （配置文件里读来的整串标题因此可以直接传，不会显示 {@code Missing message}）；占位符按 {@code {0}}、{@code {1}}… 顺序替换，
     * 其余文案也可用 {@link #text(String, Object...)}。
     *
     * @param messages 语言服务；传 {@code null} 表示不做翻译，键本身就是要显示的文本
     */
    protected Menu(Player viewer, MessageService messages, int size, String title, Object... titleArgs) {
        this.viewer = Objects.requireNonNull(viewer, "viewer");
        this.messages = messages;
        this.size = normalizeSize(size);
        this.holder = new MenuHolder();
        // holder 先于 inventory 赋值：createInventory 需要 holder，而 holder.getInventory() 读的正是下面这个字段
        this.inventory = Bukkit.createInventory(holder, this.size, textOr(title, title, titleArgs));
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

    /** 容器大小（槽位数）。 */
    public final int size() {
        return size;
    }

    /**
     * 打开界面：第一次打开时才构建内容（此后 {@link #refresh()} 随叫随到）。
     * <p>
     * 因此子类<b>不必</b>在构造里调 {@link #refresh()}——基类构造期间子类字段还没赋值，那时候构建读到的是 null。
     */
    public final void open() {
        if (!built) {
            refresh();
        }
        viewer.openInventory(inventory);
    }

    /** 关掉这个界面（玩家按 ESC 也是同样效果）。 */
    public final void close() {
        viewer.closeInventory();
    }

    // ---------- 子类接口 ----------

    /**
     * 填充物品。由 {@link #refresh()} 调用，可能在任意时刻被反复调用，
     * 因此实现里要按当前真实数据重新算一遍，不能依赖上一次的结果。
     */
    protected abstract void build();

    /**
     * 在指定槽位放一个菜单项。
     * <p>
     * 越界或 {@code null} 会被静默忽略：界面数据可能来自配置与数据库，
     * 不该因为一条脏数据（目标数超出预留区域）就抛异常把整个界面打断。
     */
    protected final void set(int slot, MenuItem item) {
        if (item == null || slot < 0 || slot >= size) {
            return;
        }
        items.put(slot, item);
        inventory.setItem(slot, item.icon());
    }

    /**
     * 声明界面布局：在 {@link #build()} 开头把界面画成一张文本图（见 {@link SlotLayout}），
     * 之后就能用 {@link #set(String, MenuItem)} 按名字摆位。
     * <p>
     * 布局表里一个字符就是一格、空格是空位，因此它读起来就是界面本身；一个名字可以占多格，
     * 多格的那种就是动态槽位（见 {@link #fill(String, List)}）。
     */
    protected final void layout(String... rows) {
        this.layout = SlotLayout.parse(size, rows);
    }

    /** 这个名字占的格子下标，按阅读顺序；没声明布局、或名字不在表里都抛（写错必须当场炸）。 */
    protected final List<Integer> slots(String name) {
        if (layout == null) {
            throw new IllegalStateException("还没声明布局就按名字取槽位「" + name + "」：先在 build() 里调用 layout(...)");
        }
        return layout.slots(name);
    }

    /** 布局里有没有这个名字（可选摆位用：可能被关掉的分页按钮、可选标签，布局里没写就别摆）。 */
    protected final boolean declared(String name) {
        return layout != null && layout.names().contains(name);
    }

    /** 静态槽位：这个名字占的每一格都放同一个物品（单格就是常规用法）；名字写错会抛。 */
    protected final void set(String name, MenuItem item) {
        for (int slot : slots(name)) {
            set(slot, item);
        }
    }

    /**
     * 动态槽位：把一串物品按顺序填进这个名字占的格子（任务列表、候选列表这种）。
     * 物品比格子多时多的不显示，物品比格子少时剩下的格子留空（由 {@link #fill(MenuItem)} 铺底）。
     */
    protected final void fill(String name, List<MenuItem> items) {
        if (layout == null) {
            throw new IllegalStateException("还没声明布局就按名字填槽位「" + name + "」：先在 build() 里调用 layout(...)");
        }
        List<Integer> slots = layout.take(name, items == null ? 0 : items.size());
        for (int index = 0; index < slots.size(); index++) {
            set(slots.get(index), items.get(index));
        }
    }

    /** 用同一个菜单项填满尚未占用的空位（背景板、禁用态按钮的铺底）。 */
    protected final void fill(MenuItem item) {
        for (int slot = 0; slot < size; slot++) {
            if (!items.containsKey(slot)) {
                set(slot, item);
            }
        }
    }

    /**
     * 清空后重跑 {@link #build()}，用于操作后即时刷新界面。
     * <p>
     * 正在重建时再次调用会直接返回：动作里刷新、{@code build()} 里又刷新这类写法
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

    // ---------- 供监听器使用（包内可见，不对外暴露） ----------

    /** 取槽位上的菜单项，没有则返回 {@code null}。 */
    final MenuItem itemAt(int slot) {
        return items.get(slot);
    }

    // ---------- 文本 ----------

    /**
     * 渲染语言键文本（占位符按 {0}、{1}… 顺序替换）。
     * <p>
     * 接了语言服务时按玩家客户端语言解析（与聊天提示保持同一语言）；没接时键本身就是要显示的字。
     */
    protected final String text(String key, Object... args) {
        if (messages == null) {
            return literal(key, args);
        }
        return TextRenderer.render(messages.raw(viewer, key, args));
    }

    /**
     * 有语言键就用它，没有就用自带文案——库自带的默认文案（翻页按钮这类）用它：
     * 宿主定义了自己的键就自动生效，没定义也不会在界面上显示「Missing message: ...」。
     */
    protected final String textOr(String key, String fallback, Object... args) {
        return messages != null && messages.has(key) ? text(key, args) : literal(fallback, args);
    }

    /** 渲染一段成品文本并替换占位符（不走语言文件）。 */
    private static String literal(String template, Object... args) {
        return TextRenderer.render(format(template, args));
    }

    /** 没接语言服务时的占位符替换；未提供的占位符静默移除。 */
    private static String format(String template, Object... args) {        if (template == null || args == null || args.length == 0) {
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
        Menu menu() {
            return Menu.this;
        }
    }
}
