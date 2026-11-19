package cn.yvmou.ylib.logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 控制台日志。**只管日志,不管游戏内消息**。
 *
 * <h2>形状取自 SLF4J,但不依赖它</h2>
 * 方法名与 {@code {}} 占位符语义跟 SLF4J 一致(见 §「为什么不是 SLF4J 封装」),但不引入
 * {@code org.slf4j} —— 原因有三,每一条都是硬约束:
 * <ol>
 *   <li><b>Spigot / 原版 Bukkit 上没有 SLF4J。</b>Paper 是单独打补丁才让插件用得上它的
 *       ("Allow plugins to use SLF4J for logging");社区还需要 {@code bukkit-slf4j} 这种桥接项目,
 *       恰恰说明它不是标配。而本库的起点是 1.13。</li>
 *   <li><b>SLF4J 的 {@code ServiceLoader} 绑定与 shadowJar 相克。</b>宿主按 README 做
 *       {@code relocate 'cn.yvmou.ylib', ...} 时不会(也不该)改 {@code org.slf4j};
 *       于是库不能自带 {@code slf4j-api}(会与服务器那一份冲突,或 relocate 出一个找不到 backend 的
 *       第二个 SLF4J 世界),只能指望服务器提供 —— 在 Spigot 上就是崩。</li>
 *   <li><b>会丢掉 {@code debug} 总开关。</b>SLF4J 里对应的是「动态调整服务器的日志级别」,
 *       而 Spigot 根本没有日志级别配置。库侧的「是否输出调试日志」只能自己管。</li>
 * </ol>
 * 借形状、不借依赖,换来的是「日志能不能工作」不取决于服务器实现。
 *
 * <h2>为什么没有 {@code ChatColor} 参数</h2>
 * 颜色是**输出格式**,不是 API 语义:{@code warn} 就是 warn。让调用方传颜色会把 Bukkit 的
 * 表现细节泄漏进公开签名,也让「日志级别」与「聊天颜色」这两个概念纠缠在一起。
 * 实现自己按级别决定怎么写(见 {@code LoggerImpl})。
 *
 * <h2>异常请用 {@link #error(Throwable, String, Object...)}</h2>
 * 把 {@code Throwable} 混在占位符参数里(靠 {@code instanceof} 去猜)会让「这条日志带堆栈吗」
 * 变得不可见。独立的参数位把这件事写在签名上。
 */
public interface Logger {

    /**
     * 调试日志。默认**输出**，宿主可用 {@code PluginInfo.setLoggerDebug(false)} 关掉（见 {@code 文档/日志.md}）。
     * <p>
     * 与 {@code info} 的区别不在级别,而在「要不要让最终用户看到」——调试日志是给开发者看的。
     */
    void debug(@NotNull String format, @NotNull Object... args);

    /** 信息日志。 */
    void info(@NotNull String format, @NotNull Object... args);

    /** 警告日志。 */
    void warn(@NotNull String format, @NotNull Object... args);

    /** 错误日志(不带堆栈)。 */
    void error(@NotNull String format, @NotNull Object... args);

    /**
     * 错误日志，并把 {@code cause} 的堆栈写进控制台。
     * <p>
     * 堆栈**只进控制台**：它是给运维排查用的，不该出现在游戏内。
     * <p>
     * <b>只记一条错误、没有异常对象时请用 {@link #error(String, Object...)}</b>。
     * 想在这个重载上传 {@code null} 必须写成 {@code error((Throwable) null, "…")} ——
     * 字面量 {@code null} 会在它与 {@code error(String, Object...)} 之间产生歧义（两个重载都适用），
     * 这是 Java 重载解析的结果，不是可以绕过的实现细节。
     */
    void error(@Nullable Throwable cause, @NotNull String format, @NotNull Object... args);
}
