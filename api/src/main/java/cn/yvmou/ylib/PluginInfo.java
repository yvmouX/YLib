package cn.yvmou.ylib;

/**
 * 插件身份信息：名字、聊天前缀、版本，以及日志用的前缀与调试开关。
 *
 * <h2>为什么全是静态</h2>
 * 日志与消息发生在任何东西都可能还没装配好的时候（装配期自己就要打日志），
 * 所以这部分身份的读取必须是「不依赖任何实例」的。
 *
 * <h2>未初始化时的读取是安全的</h2>
 * {@link #getLoggerPrefix()} 与 {@link #getPluginPrefix()} 在 {@code YLib.init} 之前会返回
 * 一个**兜底串**而不是 {@code null}。原因不是洁癖：装配期真的存在早于 {@code YLib.init} 的日志。
 * 典型路径是配置版本迁移 ——
 * {@code ConfigurationManager.registerConfiguration} 会先跑迁移再创建实例，
 * 而迁移的进度是通过 {@code ConfigurationLoader} 播报的，它拿到的 {@code Logger}
 * 由 {@code CoreServices} 创建，那一刻 {@code PluginInfo} 还是空的。
 * 若返回 {@code null}，日志行会变成 {@code null[WARN] ...}，而消息前缀会拼出 {@code null}。
 * <p>
 * 兜底串刻意不是「空串」：日志里出现一个看得见的占位名，比一整行没有归属要好排查。
 */
public final class PluginInfo {

    /** 未初始化时的日志归属名。 */
    private static final String UNKNOWN = "YLib";

    private static String pluginName = null;
    private static String pluginPrefix = null;
    private static String pluginVersion = null;

    /** 日志是否输出 {@code debug} 级别；默认开。 */
    private static volatile boolean loggerDebug = true;

    /** 日志行前缀（不含方括号），默认取插件名。 */
    private static String loggerPrefix = null;

    private PluginInfo() {
    }

    // ---------- 写入（装配期与宿主配置用） ----------

    /** 仅供 YLib 初始化时写入。 */
    static void setPluginName(String name) {
        pluginName = name;
    }

    /** 仅供 YLib 初始化时写入。 */
    static void setPluginPrefix(String prefix) {
        pluginPrefix = prefix;
    }

    /** 仅供 YLib 初始化时写入。 */
    static void setPluginVersion(String version) {
        pluginVersion = version;
    }

    /**
     * 设置日志行前缀（不含方括号）。
     * <p>
     * 由 {@code YLib.initializePluginInfo()} 写入；对宿主的测试可见是刻意的 ——
     * 单测要在没有服务端的情况下断言日志格式，就得能把前缀设成已知值。
     */
    public static void setLoggerPrefix(String prefix) {
        loggerPrefix = prefix;
    }

    /**
     * 是否输出 {@code debug} 级别日志。
     * <p>
     * 这是库侧的闸门，与服务器的日志级别无关 —— 也正因为如此，它不能被「交给服务器配置」替代
     * （Spigot 根本没有日志级别配置）。宿主应当在 {@code onEnable} 里按自己的调试开关调一次。
     */
    public static void setLoggerDebug(boolean debug) {
        loggerDebug = debug;
    }

    // ---------- 读取（任何时刻都安全） ----------

    /** 插件名；未初始化时返回 {@link #UNKNOWN}。 */
    public static String getPluginName() {
        return pluginName == null ? UNKNOWN : pluginName;
    }

    /** 聊天前缀（含颜色码与方括号）；未初始化时给一个不带颜色的兜底。 */
    public static String getPluginPrefix() {
        return pluginPrefix == null ? "[" + UNKNOWN + "] " : pluginPrefix;
    }

    /** 插件版本；未初始化时为 {@code null}（版本号没有合理的兜底值）。 */
    public static String getPluginVersion() {
        return pluginVersion;
    }

    /** 日志是否输出 debug 级别。 */
    public static boolean getLoggerDebug() {
        return loggerDebug;
    }

    /** 日志行前缀（不含方括号）；未初始化时返回 {@link #UNKNOWN}。 */
    public static String getLoggerPrefix() {
        return loggerPrefix == null ? UNKNOWN : loggerPrefix;
    }
}
