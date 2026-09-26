package cn.yvmou.ylib.menu;

/**
 * 一个菜单模板：**布局 + 声明**，就这两件事。
 *
 * <pre>{@code
 * @MenuConfig(path = "gui/adminmenu.yml", version = 1)
 * public final class AdminMenu implements MenuTemplate {
 *
 *     private final QuestService questService;
 *
 *     public AdminMenu(QuestService questService) {
 *         this.questService = questService;
 *     }
 *
 *     @Override
 *     public String[] layout() {
 *         return new String[]{
 *             "#########",
 *             "##`quests`#c#`create`##",
 *             "#########"
 *         };
 *     }
 *
 *     @Override
 *     public void declare(MenuBuilder b) {
 *         b.symbol('#', "border").of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "));
 *         b.symbol("quests", "quests_btn")
 *             .of(Items.of(Material.WRITABLE_BOOK).name("&f任务列表"))
 *             .ph("count", ctx -> String.valueOf(questService.count(ctx.viewer())))
 *             .onClick((slot, ctx) -> new QuestListMenu(questService).open(ctx.viewer()));
 *         b.fillEmpty(Items.filler());
 *     }
 * }
 *
 * // 装配处（onEnable）：注册一次，之后用模板实例打开
 * Menus.register(plugin, new AdminMenu(questService));
 * new AdminMenu(questService).open(player);
 * }</pre>
 *
 * <h2>生命周期：{@code layout()} 与 {@code declare()} 只在注册期各跑一次</h2>
 * 注册时解析布局、执行声明、跑完全部校验，产出一个**不可变**的 {@code MenuDefinition} 全局复用。
 * 由此推出一条硬约束：<b>「按玩家不同」只能写在 {@code ph()} / {@code visible()} / {@code onClick()}
 * 内部，不能写在 {@code declare()} 的 {@code if} 分支里</b>——后者在注册期就定型了，之后永远不变。
 * <p>
 * 与之配套的是渲染：每次 {@code open()} / {@code refresh()} 都求值占位符、重新构造
 * {@code ItemStack}，因此动态数值永远是新的。
 *
 * <h2>为什么模板是接口，而 {@code open()} 是 default 方法</h2>
 * {@link #layout()} 是实例方法（模板可以持有 Service 依赖），因此模板本来就是「一个实例」；
 * {@link #open} 在本实例已被 {@code Menus.register} 注册过时可用。把注册与打开分成两步，
 * 是为了让「解析校验」有唯一的、发生在启动期的入口——那正是本 API 最重要的一条保证。
 */
public interface MenuTemplate {

    /**
     * 布局表：一行字符串 = 一行槽位（见重构文档 §3）。
     * <p>
     * 单字符符号裸写、多字符符号用反引号包裹（{@code `quests`}）、空白只用于源码对齐；
     * 行数 1..6，每行格数必须等于 {@link #columns()}。
     */
    String[] layout();

    /**
     * 声明每个符号的物品与行为。
     * <p>
     * 布局里出现的每个符号都必须在这里声明，否则注册期报错并指出未知符号与它首次出现的列号。
     */
    void declare(MenuBuilder b);

    /** 每行格数；箱子界面固定 9，改成别的值只用于自测布局解析。 */
    default int columns() {
        return 9;
    }

    /**
     * 标题模板：支持 {@code &} 色码与 {@code {占位符}}。
     * <p>
     * 标题在容器创建时定稿（原版做不到打开后改标题），而容器是**每次打开**新建的，
     * 因此这里的占位符按「打开时」求值是可行的——需要按玩家变的标题（玩家名、当前页码）
     * 必须走 {@link MenuBuilder#titleVars} 提供的取值，而不是在里面读自己的字段。
     */
    default String title() {
        return "";
    }

    // ---------- 便捷转发 ----------

    /** 给某个玩家打开这个菜单；在本模板已注册的前提下可用（否则抛并提示先 {@code Menus.register}）。 */
    default MenuSession open(org.bukkit.entity.Player viewer) {
        return Menus.open(this, viewer);
    }

    /** 关掉某个玩家正开着的这个菜单。 */
    default void close(org.bukkit.entity.Player viewer) {
        Menus.close(this, viewer);
    }

    /** 原地重渲染某个玩家正开着的这个菜单。 */
    default void refresh(org.bukkit.entity.Player viewer) {
        Menus.refresh(this, viewer);
    }
}
