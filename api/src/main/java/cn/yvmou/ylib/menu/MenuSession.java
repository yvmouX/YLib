package cn.yvmou.ylib.menu;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * **「玩家 × 菜单」维度的持久状态**。
 * <p>
 * 关键约定：{@code close()} <b>不销毁</b> session，只标记「当前未打开」。这是分页菜单能不能用的分水岭——
 * 玩家点进详情再返回列表，理应还停在第 3 页；如果 session 跟着一次「打开」走，页码就必然丢失。
 * <table>
 *   <caption>生命周期</caption>
 *   <tr><th>事件</th><th>对 session 的影响</th></tr>
 *   <tr><td>首次 open</td><td>新建（page = 0）</td></tr>
 *   <tr><td>close</td><td><b>保留</b>，只标记未打开</td></tr>
 *   <tr><td>再次 open</td><td><b>复用</b>，页码停在关闭前的位置</td></tr>
 *   <tr><td>玩家退出登录</td><td>立即回收</td></tr>
 *   <tr><td>超过 {@code @MenuConfig.sessionTtl}</td><td>按需回收（默认 30 分钟）</td></tr>
 *   <tr><td>{@code resetSession(player)}</td><td>显式清空（回第一页、丢自定义键）</td></tr>
 * </table>
 * <b>内存边界</b>：{@link #put} 只该放轻量值（页码、枚举、字符串）。塞 {@code List<ItemStack>} 或数据库
 * 结果等于把玩家数据常驻内存——50 人 × 20 个菜单就是一份稳定的泄漏。这条不做运行时强制
 * （类型是 {@code Object}），但它是本类的使用契约。
 */
public final class MenuSession {

    /** 页码的键：{@link #page()} / {@link #page(int)} 走它，与自定义键共用一张表但不冲突。 */
    private static final String PAGE_KEY = "page";

    /** 一个玩家在这个菜单上的状态。 */
    private static final class State {

        private final Map<String, Object> values = new ConcurrentHashMap<String, Object>();
        private volatile long lastAccess = System.currentTimeMillis();
    }

    /** 菜单身份（模板类名） → 玩家 UUID → 状态。 */
    private static final Map<String, Map<UUID, State>> SESSIONS =
            new ConcurrentHashMap<String, Map<UUID, State>>();

    /**
     * 此刻开着某个菜单的玩家。
     * <p>
     * 「开没开着」这件事不能存在 {@link State} 里：{@link #of} 每次都新建一个 {@code MenuSession}
     * 视图对象，状态存在它身上会导致「同一个玩家的两次取用看到不同的开关值」。因此它放在
     * 按菜单身份索引的静态表里，与 session 走同一套回收。
     */
    private static final Map<String, Set<UUID>> OPEN = new ConcurrentHashMap<String, Set<UUID>>();

    private final String menuKey;
    private final UUID viewer;

    private MenuSession(String menuKey, UUID viewer) {
        this.menuKey = menuKey;
        this.viewer = viewer;
    }

    // ---------- 取用 ----------

    /**
     * 取（必要时新建）某个玩家在这个菜单上的 session。
     * <p>
     * 每次都刷新「最后访问时间」并顺带回收过期的其它 session——把 TTL 的活干在访问路径上，
     * 就不必为了回收再挂一个全局重复任务（那是每个菜单多一份常驻任务的写法）。
     */
    public static MenuSession of(String menuKey, Player player, long ttlSeconds) {        long now = System.currentTimeMillis();
        Map<UUID, State> byViewer = SESSIONS.get(menuKey);
        if (byViewer == null) {
            Map<UUID, State> created = new ConcurrentHashMap<UUID, State>();
            Map<UUID, State> existing = SESSIONS.putIfAbsent(menuKey, created);
            byViewer = existing == null ? created : existing;
        }
        harvest(byViewer, ttlSeconds, now);
        State state = byViewer.get(player.getUniqueId());
        if (state == null) {
            State created = new State();
            State existing = byViewer.putIfAbsent(player.getUniqueId(), created);
            state = existing == null ? created : existing;
        }
        state.lastAccess = now;
        return new MenuSession(menuKey, player.getUniqueId());
    }

    /** 丢掉某个玩家在这个菜单上的全部状态（{@code resetSession} / 退出登录）。 */
    static void drop(String menuKey, UUID viewer) {
        Map<UUID, State> byViewer = SESSIONS.get(menuKey);
        if (byViewer != null) {
            byViewer.remove(viewer);
        }
        Set<UUID> opened = OPEN.get(menuKey);
        if (opened != null) {
            opened.remove(viewer);
        }
    }

    /** {@link #drop} 的公开入口：宿主拿到菜单 key（如 {@code MenuDefinition#key()}）时可以直接清。 */
    public static void dropFor(String menuKey, UUID viewer) {
        if (menuKey != null && viewer != null) {
            drop(menuKey, viewer);
        }
    }

    /** 标记「这个玩家此刻开着这个菜单」，由派发层在打开 / 关闭事件里维护。 */
    public static void markOpenFor(String menuKey, UUID viewer, boolean open) {
        Set<UUID> opened = OPEN.get(menuKey);
        if (open) {
            if (opened == null) {
                Set<UUID> created = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
                Set<UUID> existing = OPEN.putIfAbsent(menuKey, created);
                opened = existing == null ? created : existing;
            }
            opened.add(viewer);
        } else if (opened != null) {
            opened.remove(viewer);
        }
    }

    /** 丢掉某个玩家在**所有**菜单上的状态（退出登录时调它，比逐个菜单清干净）。 */
    public static void dropAll(UUID viewer) {
        if (viewer == null) {
            return;
        }
        for (Map<UUID, State> byViewer : SESSIONS.values()) {
            byViewer.remove(viewer);
        }
        for (Set<UUID> opened : OPEN.values()) {
            opened.remove(viewer);
        }
    }

    /** 清空全部 session（插件停用 / 重载时调它，避免静态表把旧插件的玩家数据留着）。 */
    public static void clear() {
        SESSIONS.clear();
        OPEN.clear();
    }

    /** 回收超过 TTL 没被访问过的状态。 */
    private static void harvest(Map<UUID, State> byViewer, long ttlSeconds, long now) {
        if (ttlSeconds <= 0) {
            return;
        }
        long deadline = now - ttlSeconds * 1000L;
        for (Map.Entry<UUID, State> entry : byViewer.entrySet()) {
            if (entry.getValue().lastAccess < deadline) {
                byViewer.remove(entry.getKey());
            }
        }
    }

    // ---------- 状态 ----------

    /** 当前页，0 起。 */
    public int page() {
        State state = state();
        Object value = state == null ? null : state.values.get(PAGE_KEY);
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }

    /**
     * 写入当前页。只校验 {@code >= 0}：<b>上界由 {@link MenuContext#pageCount()} 决定</b>，
     * 框架不知道总条数，因此不做 clamp——用 {@code catch} 越界来代替边界判断是错的做法。
     */
    public void page(int page) {
        if (page < 0) {
            throw new MenuConfigException("页码不能是负数：" + page);
        }
        touch();
        State state = state();
        if (state != null) {
            state.values.put(PAGE_KEY, Integer.valueOf(page));
        }
    }

    /** 取一个自定义键；没有过则为 {@code null}。 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        State state = state();
        return state == null ? null : (T) state.values.get(key);
    }

    /** 写一个自定义键；值只该是轻量值（页码、枚举、字符串），见类注释。 */
    public void put(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new MenuConfigException("session 的键不能为空");
        }
        touch();
        State state = state();
        if (state == null) {
            return;
        }
        if (value == null) {
            state.values.remove(key);
        } else {
            state.values.put(key, value);
        }
    }

    /** 清空：回第一页并丢掉全部自定义键。 */
    public void reset() {
        State state = state();
        if (state != null) {
            state.values.clear();
        }
    }

    /** 这个玩家此刻是否开着这个菜单（由派发层在打开 / 关闭事件里维护）。 */
    public boolean isOpen() {
        return isOpenFor(menuKey, viewer);
    }

    /** 某个玩家此刻是否开着某个菜单；给「没法拿到 session 实例」的地方用。 */
    public static boolean isOpenFor(String menuKey, UUID viewer) {
        Set<UUID> opened = OPEN.get(menuKey);
        return opened != null && opened.contains(viewer);
    }

    private State state() {
        Map<UUID, State> byViewer = SESSIONS.get(menuKey);
        return byViewer == null ? null : byViewer.get(viewer);
    }

    private void touch() {
        State state = state();
        if (state != null) {
            state.lastAccess = System.currentTimeMillis();
        }
    }
}
