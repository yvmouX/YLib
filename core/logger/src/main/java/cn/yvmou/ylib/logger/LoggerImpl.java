package cn.yvmou.ylib.logger;

import cn.yvmou.ylib.PluginInfo;
import cn.yvmou.ylib.utils.LegacyCodes;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * 控制台日志的实现。四条方法**只有一处出口**，因此不会再出现「info 走聊天、warn 走日志」的错位。
 *
 * <h2>为什么统一走 {@link java.util.logging}</h2>
 * 之前 {@code info}/{@code debug} 发 {@code ConsoleSender.sendMessage(...)}，而 {@code warn}/{@code error}
 * 走 {@code Logger#log(...)} —— 同一个 Logger 的四个方法，一半拿到的是「聊天消息」的待遇
 * （带色码、能被别人的事件监听改写），一半拿到的是真日志的待遇（时间戳、级别、可被日志系统过滤）。
 * 现在四条都走 {@code Logger#log(Level, String)}：时间戳、级别、日志系统过滤统一生效，
 * 色码在写入前用 {@link LegacyCodes#strip} 剥掉。
 *
 * <h2>{@code debug} 为什么用 {@link Level#INFO} 而不是 {@link Level#FINE}</h2>
 * 因为 {@code FINE} 会被过滤掉、什么也看不见。实测：根 logger 与 {@code ConsoleHandler}
 * 的默认级别都是 {@code INFO}，{@code log(FINE, …)} 在<b>到达任何 handler 之前</b>就被 JUL 丢弃了。
 * 也就是说「用级别过滤 debug」在 Bukkit 上不成立 —— 而 {@code FINE} 与 {@code INFO} 在日志里
 * 长得也分不出来（默认格式只写级别名）。
 * <p>
 * 因此 {@code debug} 走 {@code INFO} 级别、靠消息里的 {@code [DEBUG]} 标签区分，
 * 开关**只由** {@link cn.yvmou.ylib.PluginInfo#getLoggerDebug()} 负责。
 * 代价是服主不能用日志级别单独过滤掉调试行（只能按文本过滤），换来的是「开了开关就一定看得见」。
 */
public class LoggerImpl implements Logger {

    public LoggerImpl() {
    }

    // ---------- 四条日志 ----------

    @Override
    public void debug(@NotNull String format, @NotNull Object... args) {
        // 库侧开关是唯一的闸门：关掉调试时连格式化都不做
        if (!PluginInfo.getLoggerDebug()) {
            return;
        }
        write(Level.INFO, "DEBUG", format, args);
    }

    @Override
    public void info(@NotNull String format, @NotNull Object... args) {
        write(Level.INFO, "INFO", format, args);
    }

    @Override
    public void warn(@NotNull String format, @NotNull Object... args) {
        write(Level.WARNING, "WARN", format, args);
    }

    @Override
    public void error(@NotNull String format, @NotNull Object... args) {
        write(Level.SEVERE, "ERROR", format, args);
    }

    @Override
    public void error(@Nullable Throwable cause, @NotNull String format, @NotNull Object... args) {
        write(Level.SEVERE, "ERROR", format, args);
        if (cause != null) {
            // 堆栈单独写一条：这样「一条日志一行」的格式不被破坏，日志系统也能把堆栈收进
            // 可折叠的异常块（Paper 的 Log4j 就是这么处理的）
            Bukkit.getLogger().log(Level.SEVERE, PluginInfo.getLoggerPrefix() + "  ↑ 异常堆栈", cause);
        }
    }

    // ---------- 唯一出口 ----------

    /**
     * 把一条日志写进控制台。
     * <p>
     * 级别标签由**我们**加、而不交给日志系统：JUL 默认格式只有「级别缩写 + logger 名」，
     * 而我们要的是统一的插件前缀。因此格式固定为 {@code <插件前缀>[LEVEL] 正文}，正文里的色码先剥掉。
     */
    private void write(@NotNull Level level, @NotNull String levelName,
                       @NotNull String format, @NotNull Object... args) {
        String body = LegacyCodes.strip(LoggerUtil.formatMessage(format, args));
        Bukkit.getLogger().log(level, PluginInfo.getLoggerPrefix() + "[" + levelName + "] " + body);
    }
}
