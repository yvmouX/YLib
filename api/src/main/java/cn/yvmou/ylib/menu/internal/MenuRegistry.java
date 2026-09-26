package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.ClickContext;
import cn.yvmou.ylib.menu.ClickType;
import cn.yvmou.ylib.menu.MenuBuilder;
import cn.yvmou.ylib.menu.MenuConfig;
import cn.yvmou.ylib.menu.MenuConfigException;
import cn.yvmou.ylib.menu.MenuDefinition;
import cn.yvmou.ylib.menu.MenuSession;
import cn.yvmou.ylib.menu.MenuTemplate;
import cn.yvmou.ylib.menu.MenuView;
import cn.yvmou.ylib.menu.Menus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册表 + 事件派发：菜单框架唯一持有可变状态的地方。
 *
 * <h2>三张表</h2>
 * <ul>
 *   <li>{@link #DEFINITIONS}：模板类 → 不可变定义（注册期产出，之后只读）；</li>
 *   <li>{@link #VIEWS}：玩家 → 他此刻开着的那个 {@link MenuView}。归属判定**不靠它**——
 *       点击事件从 {@link InventoryHolder} 拿 {@link MenuView}（对象身份，不会认错别的插件的容器），
 *       这张表只用于「玩家退服时清 session」这种按玩家查的场景；</li>
 *   <li>监听器实例：按插件登记，避免重复注册。</li>
 * </ul>
 *
 * <h2>为什么归属判定必须靠 holder 身份</h2>
 * 标题会随标题占位符变化、也可能与别的插件的容器重名，因此它只能当展示数据，不能当身份。
 * {@link MenuView} 本身就是 holder，{@code event.getView().getTopInventory().getHolder()} 拿到它
 * 就等于「这个容器确定是本库的」。
 */
public final class MenuRegistry implements Listener {

    /** 模板类 → 定义，按注册顺序保留。 */
    private static final Map<Class<?>, MenuDefinition> DEFINITIONS =
            new LinkedHashMap<Class<?>, MenuDefinition>();

    /**
     * 模板类 → 当初登记的那个实例。
     * <p>
     * 热重载要靠它重跑 {@code declare()}。存实例而不是类，是因为模板可以持有 Service 依赖，
     * 而重建必须从**同一个实例**出发（临时 new 一个可能拿不到那些依赖）。
     */
    private static final Map<Class<?>, MenuTemplate> TEMPLATES =
            new LinkedHashMap<Class<?>, MenuTemplate>();

    /** 玩家 → 他此刻开着的那个菜单视图。 */
    private static final Map<UUID, MenuView> VIEWS = new ConcurrentHashMap<UUID, MenuView>();

    /** 定义 → 登记它的插件。按定义的对象身份索引，用来定位调度器。 */
    private static final Map<MenuDefinition, Plugin> PLUGINS =
            new java.util.IdentityHashMap<MenuDefinition, Plugin>();

    /** 已登记事件监听器的插件。 */
    private static final Map<Plugin, Boolean> LISTENERS = new ConcurrentHashMap<Plugin, Boolean>();

    /** {@code InventoryCloseEvent#getReason()}；老服务端上没有这个方法，因此探测一次并缓存。 */
    private static final java.lang.reflect.Method GET_REASON = findGetReason();

    private static java.lang.reflect.Method findGetReason() {
        try {
            return InventoryCloseEvent.class.getMethod("getReason");
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }

    private MenuRegistry() {
    }

    // ---------- 注册 ----------

    /** 登记事件派发；同一个插件只登记一次。 */
    public static void init(Plugin plugin) {
        if (plugin == null) {
            throw new MenuConfigException("Menus.init(plugin) 的 plugin 是 null");
        }
        if (LISTENERS.putIfAbsent(plugin, Boolean.TRUE) != null) {
            return;
        }
        try {
            Bukkit.getPluginManager().registerEvents(new MenuRegistry(), plugin);
        } catch (RuntimeException noServer) {
            // 单测里没有服务端：注册不了不该成为失败原因（界面本身不需要监听器也能构建）
            LISTENERS.remove(plugin);
        }
    }

    /** 扫描 {@code @MenuConfig}、解析、校验、读 yml，产出并缓存不可变定义。 */
    public static MenuDefinition register(Plugin plugin, MenuTemplate template) {
        if (template == null) {
            throw new MenuConfigException("Menus.register 的模板是 null");
        }
        Class<?> type = template.getClass();
        MenuDefinition existing = DEFINITIONS.get(type);
        if (existing != null) {
            // 同一个模板类注册两次没有第二个答案；也顺带挡住「两个实例各拿一份定义」的意外
            return existing;
        }
        MenuDefinition definition = rebuild(plugin, template);
        TEMPLATES.put(type, template);
        DEFINITIONS.put(type, definition);
        if (plugin != null) {
            PLUGINS.put(definition, plugin);
        }
        return definition;
    }

    /**
     * 跑一遍「声明 → 读 yml →（按文件里的布局）解析 → 合并 items」，产出定义并逐条记录 warning。
     * <p>
     * 注册与热重载共用这一条路径——两者的差别只在「产出之后要不要覆盖缓存」，
     * 分成两份实现的话，校验规则迟早会在其中一条上漂移。
     * <p>
     * <b>顺序不能换</b>：文件里的 {@code layout} 决定容器怎么切，而定义一旦 build 出来就定稿了，
     * 所以「读文件」必须排在 {@code build} 之前；物品覆盖反过来要在 build 之后（它改的是定义里的物品）。
     */
    private static MenuDefinition rebuild(Plugin plugin, MenuTemplate template) {
        MenuConfig config = template.getClass().getAnnotation(MenuConfig.class);
        MenuBuilder builder = new MenuBuilder();
        template.declare(builder);

        YamlConfiguration yaml = config == null ? null
                : MenuConfigLoader.readAll(plugin, template, builder.symbols(), config);
        String[] rows = yaml == null ? template.layout()
                : MenuConfigLoader.readLayout(yaml, config.path(), template, builder.symbols().keySet());
        MenuDefinition definition = builder.build(template, config, rows);
        if (yaml != null) {
            MenuConfigLoader.readItems(plugin, definition, yaml, config);
        }
        for (String warning : definition.warnings()) {
            MenuLog.warn(template.getClass().getSimpleName() + ": " + warning);
        }
        return definition;
    }

    // ---------- 热重载 ----------

    /**
     * 重读某个模板的配置并重建定义。
     * <p>
     * 已经开着这个菜单的玩家会被就地切到新定义上（容器大小不变时只是重画，不闪屏、不丢光标上的物品）；
     * 如果**布局行数改了**，容器大小在创建时就定稿了，切不过去 —— 那时记一条 warning，
     * 让那个界面保持旧定义直到玩家自己重开（而不是粗暴地关掉他的界面）。
     *
     * @return 新定义；这个模板没注册过则返回 {@code null}（重载不该凭空注册一个菜单）
     */
    public static MenuDefinition reload(Plugin plugin, MenuTemplate template) {
        if (template == null) {
            return null;
        }
        Class<?> type = template.getClass();
        MenuDefinition previous = DEFINITIONS.get(type);
        if (previous == null) {
            return null;
        }
        MenuTemplate registered = TEMPLATES.containsKey(type) ? TEMPLATES.get(type) : template;
        Plugin owner = plugin != null ? plugin : PLUGINS.get(previous);

        MenuDefinition replacement = rebuild(owner, registered);
        DEFINITIONS.put(type, replacement);
        if (owner != null) {
            PLUGINS.put(replacement, owner);
        }
        if (previous != replacement) {
            PLUGINS.remove(previous);
        }
        reattach(previous, replacement);
        return replacement;
    }

    /**
     * 重载全部已注册的菜单。
     *
     * @return 重建成功的数量
     */
    public static int reloadAll(Plugin plugin) {
        List<MenuTemplate> snapshot = new ArrayList<MenuTemplate>(TEMPLATES.values());
        int reloaded = 0;
        for (MenuTemplate template : snapshot) {
            if (reload(plugin, template) != null) {
                reloaded++;
            }
        }
        return reloaded;
    }

    /** 把开着旧定义的界面切到新定义上；大小不一致的留着不动并提示重开。 */
    private static void reattach(MenuDefinition previous, MenuDefinition replacement) {
        if (previous == replacement) {
            return;
        }
        for (MenuView view : VIEWS.values()) {
            if (view.definition() != previous) {
                continue;
            }
            if (!view.reuse(replacement)) {
                MenuLog.warn(replacement.key() + " 的布局行数变了（容器大小 " + previous.size()
                        + " → " + replacement.size() + "）：已经开着的那个界面只能关掉重开，"
                        + "它的容器大小在打开时就定稿了");
            }
        }
    }

    /** 取定义；没注册过返回 {@code null}。 */
    public static MenuDefinition definitionOf(MenuTemplate template) {
        return template == null ? null : DEFINITIONS.get(template.getClass());
    }

    private static MenuDefinition require(MenuTemplate template) {
        MenuDefinition definition = definitionOf(template);
        if (definition == null) {
            throw new MenuConfigException(template.getClass().getName()
                    + " 还没注册：先在 onEnable 里调用 Menus.register(plugin, template)");
        }
        return definition;
    }

    /** 按「玩家 × 此菜单」取 session（必要时新建），TTL 用模板声明的 sessionTtl；未注册的菜单在这里就报。 */
    public static MenuSession sessionOf(Class<?> menu, Player viewer) {
        MenuDefinition definition = menu == null ? null : DEFINITIONS.get(menu);
        if (definition == null) {
            throw new MenuConfigException((menu == null ? "null" : menu.getName())
                    + " 还没注册：先在 onEnable 里调用 Menus.register(plugin, template)");
        }
        return MenuSession.of(definition.key(), viewer, ttlOf(definition));
    }

    /** 全部已注册定义，按注册顺序。 */
    public static List<MenuDefinition> definitions() {
        return Collections.unmodifiableList(new ArrayList<MenuDefinition>(DEFINITIONS.values()));
    }

    // ---------- 打开 / 关闭 / 刷新 ----------

    public static MenuSession open(MenuTemplate template, Player viewer, Map<String, String> titleVars) {
        return open(require(template), viewer, titleVars);
    }

    public static MenuSession open(MenuDefinition definition, Player viewer) {
        return open(definition, viewer, Collections.<String, String>emptyMap());
    }

    public static MenuSession open(MenuDefinition definition, Player viewer, Map<String, String> titleVars) {
        if (viewer == null) {
            throw new MenuConfigException("打开菜单的玩家是 null");
        }
        if (definition == null) {
            throw new MenuConfigException("打开菜单的定义是 null（模板注册过吗？）");
        }
        MenuSession session = MenuSession.of(definition.key(), viewer, ttlOf(definition));
        MenuView view = MenuView.create(definition, viewer, session, titleVars);
        VIEWS.put(viewer.getUniqueId(), view);
        MenuSession.markOpenFor(definition.key(), viewer.getUniqueId(), true);
        viewer.openInventory(view.inventory());
        return session;
    }

    public static void close(MenuTemplate template, Player viewer) {
        MenuDefinition definition = definitionOf(template);
        if (definition == null || viewer == null) {
            return;
        }
        MenuView view = VIEWS.get(viewer.getUniqueId());
        if (view != null && view.definition() == definition) {
            viewer.closeInventory();
        }
    }

    public static void refresh(MenuTemplate template, Player viewer) {
        MenuDefinition definition = definitionOf(template);
        if (definition == null || viewer == null) {
            return;
        }
        MenuView view = VIEWS.get(viewer.getUniqueId());
        if (view != null && view.definition() == definition) {
            view.refresh();
        }
    }

    public static void resetSession(MenuTemplate template, Player viewer) {
        MenuDefinition definition = definitionOf(template);
        if (definition != null && viewer != null) {
            MenuSession.dropFor(definition.key(), viewer.getUniqueId());
        }
    }

    /** 玩家此刻开着的菜单定义；没开着返回 {@code null}。 */
    public static MenuDefinition openingOf(Player viewer) {
        MenuView view = viewer == null ? null : VIEWS.get(viewer.getUniqueId());
        return view == null ? null : view.definition();
    }

    /** 玩家此刻开着的菜单视图；派发层用。 */
    public static MenuView viewOf(Player viewer) {
        return viewer == null ? null : VIEWS.get(viewer.getUniqueId());
    }

    /** 某个玩家当前所在容器归属的插件（「下一 tick 重画」这类需要调度器的操作用它）。 */
    public static Plugin pluginOf(Player viewer) {
        MenuView view = viewOf(viewer);
        return view == null ? null : PLUGINS.get(view.definition());
    }

    /** 定义 → 登记它的插件；注册时记下，没登记过返回 {@code null}。 */
    static Plugin pluginOfDefinition(MenuDefinition definition) {
        return definition == null ? null : PLUGINS.get(definition);
    }

    private static long ttlOf(MenuDefinition definition) {
        MenuConfig config = definition.config();
        return config == null ? 1800L : config.sessionTtl();
    }

    /** 卸载：清掉全部注册与 session（插件停用时调）。 */
    public static void shutdown() {
        for (Plugin plugin : new ArrayList<Plugin>(LISTENERS.keySet())) {
            try {
                HandlerList.unregisterAll(plugin);
            } catch (RuntimeException noServer) {
                // 关服中：注册表已经不存在了
            }
        }
        LISTENERS.clear();
        DEFINITIONS.clear();
        TEMPLATES.clear();
        VIEWS.clear();
        PLUGINS.clear();
        MenuSession.clear();
    }

    // ---------- 事件 ----------

    /**
     * 打开通知：{@link cn.yvmou.ylib.menu.OpenHandler} 在这里触发。
     * <p>
     * 用 {@code MONITOR}：这件事不改事件本身，只表示「玩家看到了这个界面」，放在最后跑才不会被
     * 别的插件的取消（打开事件被取消时界面其实没开）骗到。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        MenuView view = viewOf(event.getView());
        if (view == null || !(event.getPlayer() instanceof Player)) {
            return;
        }
        if (view.definition().openHandler() != null) {
            try {
                view.definition().openHandler().handle((Player) event.getPlayer(), view.inventory(),
                        view.session());
            } catch (RuntimeException failure) {
                MenuLog.warn("onOpen 回调失败: " + view.definition().key() + " -> " + failure);
            }
        }
    }

    /** 关闭通知：{@link cn.yvmou.ylib.menu.CloseHandler} 在这里触发。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        MenuView view = viewOf(event.getView());
        if (view == null || !(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        UUID id = player.getUniqueId();
        // 玩家可能立刻又开了一个菜单（换界面）：那时新视图已经登记，不能把它的登记抹掉
        MenuView current = VIEWS.get(id);
        if (current == view) {
            VIEWS.remove(id, view);
        }
        MenuSession.markOpenFor(view.definition().key(), id, false);

        if (view.definition().closeHandler() != null) {
            try {
                view.definition().closeHandler().handle(player, view.inventory(), closeReason(event),
                        view.session());
            } catch (RuntimeException failure) {
                MenuLog.warn("onClose 回调失败: " + view.definition().key() + " -> " + failure);
            }
        }
    }

    /**
     * 关闭原因。
     * <p>
     * 走反射取 {@code InventoryCloseEvent#getReason()}：那个方法是较新版本才加进 Bukkit 的，
     * 直接调用会把本库钉在某个版本上；而 {@link cn.yvmou.ylib.menu.CloseHandler} 的第三个参数
     * 本来就是 {@code Object}，取不到就传 {@code null}（「为什么关的」对绝大多数回调并不重要）。
     */
    private static Object closeReason(InventoryCloseEvent event) {
        if (GET_REASON == null) {
            return null;
        }
        try {
            return GET_REASON.invoke(event);
        } catch (ReflectiveOperationException unavailable) {
            return null;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    /** 退服：清掉这个玩家的全部 session（{@code close()} 不再销毁它，所以必须有一个明确的回收点）。 */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        VIEWS.remove(event.getPlayer().getUniqueId());
        MenuSession.dropAll(event.getPlayer().getUniqueId());
    }

    /**
     * 点击：把点击翻译成回调，并按 {@code interaction} 策略决定事件取不取消。
     * <p>
     * 判定「点的是菜单还是玩家背包」用 {@code getClickedInventory() == topInventory}，
     * <b>不用</b> {@code rawSlot < size}——后者在 {@code getView().getTopInventory()} 与
     * {@code getInventory()} 语义不同的服务端 / 代理环境下会判错。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        MenuView view = viewOf(event.getView());
        if (view == null) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        boolean inMenu = event.getClickedInventory() == top;
        ClickType clickType = ClickType.of(event.getClick());

        // 先无条件取消，再决定跑不跑回调：顺序反了，回调抛异常就会漏掉取消，物品能被拖走
        boolean cancelledByOther = event.isCancelled();
        if (shouldCancel(view, event, inMenu)) {
            event.setCancelled(true);
        }
        if (cancelledByOther || !inMenu) {
            return;
        }
        int slot = event.getSlot();
        MenuView.Rendered rendered = view.renderedAt(slot);
        if (rendered == null || rendered.handler() == null) {
            // 没注册回调（含兜底填充物）→ 等同于点到空槽
            return;
        }
        dispatch(view, event, clickType, rendered);
    }

    /** 跑一次回调：异常只记日志，不让服务端的事件处理带崩。 */
    private void dispatch(MenuView view, InventoryClickEvent event, ClickType clickType,
                          MenuView.Rendered rendered) {
        ClickContext context = new ClickContext(view, event, clickType, rendered.slot(), rendered.state());
        try {
            rendered.handler().handle(rendered.slot(), context);
        } catch (RuntimeException failure) {
            // 玩家看到的「点了没反应」如果没有日志就无从排查
            MenuLog.warn("菜单动作执行失败: " + view.definition().key()
                    + " slot=" + rendered.slot().index() + " (" + clickType + ") -> " + failure);
        }
    }

    /**
     * 这次点击要不要取消。
     *
     * @param inMenu 点击是否落在菜单容器上（而不是玩家背包）
     */
    private static boolean shouldCancel(MenuView view, InventoryClickEvent event, boolean inMenu) {
        switch (view.definition().interaction()) {
            case ALLOW_MOVE:
                // 自由整理：菜单区与背包都不拦。但玩家的背包点击本来就不需要拦
                return false;
            case ALLOW_TAKE:
                if (!inMenu) {
                    return false;
                }
                // 只允许拿走：手上拿着物品去点菜单，就是「放入」，拦下
                return event.getCursor() != null && !event.getCursor().getType().isAir();
            case READ_ONLY:
            default:
                // 菜单区一律拦；玩家背包里的普通点击放行（shift 点击会把物品塞进菜单，必须拦）
                return inMenu || event.isShiftClick();
        }
    }

    /**
     * 拖拽：{@code READ_ONLY} 下只要碰到菜单区就取消；{@code ALLOW_TAKE} 只拦「往里放」；
     * {@code ALLOW_MOVE} 全放行。
     * <p>
     * 只拦点击不拦拖拽是这类框架最常见的漏洞：拖拽能把菜单物品分配到多个槽位。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        MenuView view = viewOf(event.getView());
        if (view == null) {
            return;
        }
        boolean touchesMenu = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot < view.definition().size()) {
                touchesMenu = true;
                break;
            }
        }
        if (!touchesMenu) {
            // 整段都落在玩家背包里的拖拽交给原版：他得先把物品拖起来
            return;
        }
        switch (view.definition().interaction()) {
            case ALLOW_MOVE:
                return;
            case ALLOW_TAKE:
                if (event.getOldCursor() == null || event.getOldCursor().getType().isAir()) {
                    return;
                }
                event.setCancelled(true);
                return;
            case READ_ONLY:
            default:
                event.setCancelled(true);
        }
    }

    /**
     * 漏斗 / 其他容器想把菜单里的物品抽走：一律拦下。
     * <p>
     * 这条与 {@code interaction} 策略无关——{@link Interaction#ALLOW_MOVE} 说的是「**玩家**可以自由整理」，
     * 而不是「可以被别的容器掏空」。菜单物品本来就是插件凭空造出来的，被抽走只会变成无源之物。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        if (isMenuInventory(event.getSource()) || isMenuInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    /**
     * 玩家丢弃的物品落到菜单上：拦下。
     * <p>
     * 丢弃不经过容器、也不触发点击事件，因此只拦点击是管不住它的；而菜单里的格子不该被玩家的物品占据。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(InventoryPickupItemEvent event) {
        if (isMenuInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    private static boolean isMenuInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuView;
    }

    /** 认出视图归属的菜单；不是本库的容器返回 {@code null}。 */
    static MenuView viewOf(InventoryView view) {
        if (view == null) {
            return null;
        }
        Inventory top = view.getTopInventory();
        if (top == null) {
            return null;
        }
        InventoryHolder holder = top.getHolder();
        return holder instanceof MenuView ? (MenuView) holder : null;
    }
}
