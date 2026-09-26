package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuActionsHolder;
import cn.yvmou.ylib.menu.internal.MenuConfigLoader;
import cn.yvmou.ylib.menu.internal.MenuLog;
import cn.yvmou.ylib.menu.internal.MenuRegistry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 菜单的入口：**注册**（把模板解析成不可变定义）与**打开**。
 *
 * <pre>{@code
 * // onEnable：先注册，再使用
 * Menus.init(this);                                // 注册事件派发（不调则界面能开、点击没反应）
 * Menus.register(this, new AdminMenu(questService));   // 解析 + 校验 + 读 yml，失败即抛
 *
 * // 使用
 * Menus.open(adminMenu, player);
 * adminMenu.open(player);                          // 同一个动作的便捷写法
 * }</pre>
 *
 * <h2>为什么注册是独立的一步</h2>
 * 「解析失败必须在启动期炸，而不是玩家点开菜单时炸」是这套 API 最核心的一条保证。把注册做成
 * 显式的一步，启动期就有唯一的入口去跑完所有校验（布局、符号、perPage、材质名、yml 版本），
 * 而不是把风险留到第一次打开。{@link MenuTemplate#open} 因此要求模板**已被注册过**。
 */
public final class Menus {

    private Menus() {
    }

    // ---------- 注册与派发 ----------

    /**
     * 注册事件派发。宿主插件启用时调一次即可。
     * <p>
     * 不调也能打开界面，但点击不会有任何反应——这是本库最容易踩的坑，因此文档里把它写在第一行。
     */
    public static void init(Plugin plugin) {
        MenuRegistry.init(plugin);
    }

    /**
     * 扫描 {@code @MenuConfig}、执行 {@code layout()} / {@code declare()}、读并叠加 yml，
     * 得到一个不可变的 {@link MenuDefinition} 并缓存。
     * <p>
     * <b>任何一条校验不过都会抛 {@link MenuConfigException}，中止插件启用</b>——这是刻意的：
     * 带着一个坏菜单上线，问题只会在玩家那里以「界面打不开」的形式出现，而那时手上没有行号与列号。
     *
     * @return 这个模板的定义；重复注册同一模板类返回第一次注册的那一份
     */
    public static MenuDefinition register(Plugin plugin, MenuTemplate template) {
        return MenuRegistry.register(plugin, template);
    }

    // ---------- 打开 / 关闭 / 刷新 ----------

    /** 给某个玩家打开这个菜单；模板必须已经注册过。 */
    public static MenuSession open(MenuTemplate template, Player viewer) {
        return MenuRegistry.open(template, viewer, Collections.<String, String>emptyMap());
    }

    /** 给某个玩家打开这个菜单，并给标题的 {@code {占位符}} 提供取值。 */
    public static MenuSession open(MenuTemplate template, Player viewer, Map<String, String> titleVars) {
        return MenuRegistry.open(template, viewer, titleVars);
    }

    /** 内部用：按已解析的定义打开（{@code MenuControl.open} 走它）。 */
    public static MenuSession open(MenuDefinition definition, Player viewer) {
        return MenuRegistry.open(definition, viewer);
    }

    /** 打开菜单，并先写入这次打开要用的状态——模板全局共享，「这次看哪个任务 / 哪份草稿」只能放 session。 */
    public static MenuSession open(MenuTemplate template, Player viewer, java.util.function.Consumer<MenuSession> state) {
        return open(template, viewer, Collections.<String, String>emptyMap(), state);
    }

    /** 打开菜单：先给标题的 {@code {占位符}} 提供取值，再写入这次打开要用的状态（状态写入发生在打开前）。 */
    public static MenuSession open(MenuTemplate template, Player viewer, Map<String, String> titleVars,
                                   java.util.function.Consumer<MenuSession> state) {
        MenuSession session = session(template.getClass(), viewer);
        if (state != null) {
            state.accept(session);
        }
        return open(template, viewer, titleVars);
    }

    /** 按「玩家 × 此菜单」取 session（必要时新建）；TTL 用模板声明的 {@code sessionTtl}。 */
    public static MenuSession session(Class<?> menu, Player viewer) {
        return MenuRegistry.sessionOf(menu, viewer);
    }

    /** 关掉某个玩家正开着的这个菜单；没开着就什么都不做。 */
    public static void close(MenuTemplate template, Player viewer) {
        MenuRegistry.close(template, viewer);
    }

    /** 原地重渲染某个玩家正开着的这个菜单；没开着就什么都不做。 */
    public static void refresh(MenuTemplate template, Player viewer) {
        MenuRegistry.refresh(template, viewer);
    }

    /** 清空某个玩家在这个菜单上的持久状态（页码、筛选条件）。 */
    public static void resetSession(MenuTemplate template, Player viewer) {
        MenuRegistry.resetSession(template, viewer);
    }

    // ---------- 查询 ----------

    /** 某个模板的定义；没注册过返回 {@code null}。 */
    public static MenuDefinition definitionOf(MenuTemplate template) {
        return MenuRegistry.definitionOf(template);
    }

    /** 全部已注册的定义，按注册顺序；校验与「列出所有菜单」这类管理命令用它。 */
    public static List<MenuDefinition> definitions() {
        return MenuRegistry.definitions();
    }

    /** 这个玩家此刻开着的菜单；没开着返回 {@code null}。 */
    public static MenuDefinition openingOf(Player viewer) {
        return MenuRegistry.openingOf(viewer);
    }

    /** 卸载：清掉全部注册与 session（插件停用时调，避免静态表把旧插件的玩家数据留着）。 */
    public static void shutdown() {
        MenuRegistry.shutdown();
    }

    // ---------- 热重载 ----------

    /**
     * 重读某个菜单的配置文件并重建它的定义（{@code /xxx reload config} 调它）。
     * <p>
     * 与启动期走的是**同一条**解析 + 校验 + 合并路径，因此重载之后「写坏的那一项回退默认值」
     * 这类保证与启动期完全一致；写错到阻断级别的配置（版本比代码新、yml 语法错）仍然会抛，
     * 由调用方决定是让整个命令失败还是记日志。
     * <p>
     * 已经开着这个菜单的玩家会被**就地**切到新定义上：容器大小不变时只是重画（不闪屏、
     * 不丢光标上的物品），页码等 session 也原样保留。若**布局行数改了**，容器大小在打开时
     * 就已定稿，切不过去 —— 那时记一条 warning，让那个界面保持旧样子直到玩家自己重开。
     *
     * @return 新定义；这个模板没注册过则返回 {@code null}（重载不该凭空注册一个菜单）
     */
    public static MenuDefinition reload(Plugin plugin, MenuTemplate template) {
        return MenuRegistry.reload(plugin, template);
    }

    /**
     * 重载全部已注册的菜单。传 {@code null} 时沿用各自登记时的插件。
     *
     * @return 成功重建的数量
     */
    public static int reloadAll(Plugin plugin) {
        return MenuRegistry.reloadAll(plugin);
    }

    // ---------- 日志与声明式动作 ----------

    /** 接管菜单框架的警告出口（配置脏数据这类）；不接管就打控制台。 */
    public static void logger(java.util.function.Consumer<String> sink) {
        MenuLog.sink(sink);
    }

    /**
     * 注入「下一 tick 执行」的实现（只影响 {@code delayedRefresh(true)}）。
     * <p>
     * <b>不注入也能用</b>：默认回退到 Bukkit 调度器；拿不到调度器（单测、关服中）时就地执行一次。
     * 行为与「没这回事」时完全一致。
     *
     * <pre>{@code
     * // Folia / Canvas 上请注入平台调度器：那两个平台不允许直接用 Bukkit 调度器
     * Menus.scheduler(task -> ylib.getScheduler().runLater(task, 1L));
     * }</pre>
     *
     * 之所以做成注入而不是内部直接取平台调度器：那会把调度器（以及 {@code YLib} 本身）
     * 拽进菜单包的依赖里，而菜单包的其余部分只有 Bukkit 依赖。
     */
    public static void scheduler(java.util.function.Consumer<Runnable> nextTick) {
        cn.yvmou.ylib.menu.internal.MenuScheduler.register(nextTick);
    }

    /**
     * 登记 yml 里 {@code actions} 的宿主实现（发消息 / 执行命令 / 开另一个菜单）。
     * <p>
     * 只用了 message / close / refresh / next_page / prev_page 这类内置动作的宿主可以不登记，
     * 但那时 yml 里配了需要宿主的动作只会记一条警告。
     */
    public static void actions(MenuActions implementation) {
        MenuActionsHolder.set(implementation);
    }
}
