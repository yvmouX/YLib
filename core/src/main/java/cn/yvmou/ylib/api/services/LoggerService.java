package cn.yvmou.ylib.api.services;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * 日志服务接口
 * <p>
 * 提供不同级别的日志记录功能，支持同步/异步输出、颜色控制、文件日志等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface LoggerService {

    /**
     * 获取控制台日志前缀
     * <p>
     * 格式：§8[§b§l§n插件名§8]§r
     * </p>
     *
     * @return 格式化后的控制台日志前缀
     */
    @NotNull
    String getConsolePrefix();

    // 同步方法 - DEBUG级别

    /**
     * 记录调试级别日志（默认蓝色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void debug(@NotNull String format, @NotNull Object... args);

    /**
     * 记录调试级别日志（指定颜色）
     *
     * @param color  日志文本颜色，不能为null
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    // 同步方法 - INFO级别

    /**
     * 记录信息级别日志（默认绿色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void info(@NotNull String format, @NotNull Object... args);

    /**
     * 记录信息级别日志（指定颜色）
     *
     * @param color  日志文本颜色，不能为null
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void info(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    // 同步方法 - WARN级别

    /**
     * 记录警告级别日志（默认黄色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void warn(@NotNull String format, @NotNull Object... args);

    /**
     * 记录警告级别日志（指定颜色）
     *
     * @param color  日志文本颜色，不能为null
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void warn(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    // 同步方法 - ERROR级别

    /**
     * 记录错误级别日志（默认红色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void error(@NotNull String format, @NotNull Object... args);

    /**
     * 记录错误级别日志（指定颜色）
     *
     * @param color  日志文本颜色，不能为null
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void error(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    /**
     * 记录错误级别日志（包含异常堆栈信息）
     * <p>
     * 会自动在消息后追加异常类名和完整堆栈信息。
     * </p>
     *
     * @param format    日志格式，支持{}占位符
     * @param throwable 需要记录的异常对象
     * @param args      替换占位符的参数
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    void error(@NotNull String format, @NotNull Throwable throwable, @NotNull Object... args);

    // 异步方法

    /**
     * 异步记录调试级别日志（默认蓝色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @return 代表异步操作完成的CompletableFuture
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    @NotNull
    CompletableFuture<Void> debugAsync(@NotNull String format, @NotNull Object... args);

    /**
     * 异步记录信息级别日志（默认绿色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @return 代表异步操作完成的CompletableFuture
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    @NotNull
    CompletableFuture<Void> infoAsync(@NotNull String format, @NotNull Object... args);

    /**
     * 异步记录警告级别日志（默认黄色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @return 代表异步操作完成的CompletableFuture
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    @NotNull
    CompletableFuture<Void> warnAsync(@NotNull String format, @NotNull Object... args);

    /**
     * 异步记录错误级别日志（默认红色）
     *
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     * @return 代表异步操作完成的CompletableFuture
     * @throws IllegalArgumentException 当提供的参数数量少于占位符数量时抛出
     */
    @NotNull
    CompletableFuture<Void> errorAsync(@NotNull String format, @NotNull Object... args);

    // 文件日志方法

    /**
     * 记录日志到文件
     * <p>
     * 除了输出到控制台外，还会将日志记录到文件中。
     * 文件路径通常为：/plugins/插件名/logs/日期.log
     * </p>
     *
     * @param level  日志级别（"DEBUG"、"INFO"、"WARN"、"ERROR"）
     * @param format 日志格式，支持{}占位符
     * @param args   替换占位符的参数
     */
    void logToFile(@NotNull String level, @NotNull String format, @NotNull Object... args);

    // 日志级别控制

    /**
     * 设置日志级别
     * <p>
     * 低于设置级别的日志将不会被输出。
     * 例如：设置为WARN级别时，DEBUG和INFO级别的日志将被忽略。
     * </p>
     *
     * @param level 要设置的日志级别，不能为null
     */
    void setLogLevel(@NotNull LogLevel level);

    /**
     * 获取当前日志级别
     *
     * @return 当前设置的日志级别
     */
    @NotNull
    LogLevel getLogLevel();

    /**
     * 日志级别枚举
     * <p>
     * 级别顺序：DEBUG < INFO < WARN < ERROR
     * </p>
     */
    enum LogLevel {
        /**
         * 调试级别 - 最详细的日志信息
         */
        DEBUG,

        /**
         * 信息级别 - 一般的运行信息
         */
        INFO,

        /**
         * 警告级别 - 可能有问题的情况
         */
        WARN,

        /**
         * 错误级别 - 错误和异常情况
         */
        ERROR;

        /**
         * 检查当前级别是否高于或等于指定级别
         *
         * @param other 要比较的级别
         * @return 如果当前级别>=指定级别返回true，否则返回false
         */
        public boolean isEnabled(@NotNull LogLevel other) {
            return this.ordinal() >= other.ordinal();
        }
    }
}