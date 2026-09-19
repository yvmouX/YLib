package cn.yvmou.ylib.logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public interface Logger {

    /**
     * 发送调试日志
     * @param format 格式化字符串
     * @param args 参数
     */
    void debug(@NotNull String format, @NotNull Object... args);

    /**
     * 发送调试日志
     * @param color 颜色
     * @param format 格式化字符串
     * @param args 参数
     */
    void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    /**
     * 发送信息日志
     * @param format 格式化字符串
     * @param args 参数
     */
    void info(@NotNull String format, @NotNull Object... args);

    /**
     * 发送信息日志
     * @param color 颜色
     * @param format 格式化字符串
     * @param args 参数
     */
    void info(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    /**
     * 发送警告日志
     * @param format 格式化字符串
     * @param args 参数
     */
    void warn(@NotNull String format, @NotNull Object... args);

    /**
     * 发送警告日志
     * @param color 颜色
     * @param format 格式化字符串
     * @param args 参数
     */
    void warn(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    /**
     * 发送错误日志
     * @param format 格式化字符串
     * @param args 参数
     */
    void error(@NotNull String format, @NotNull Object... args);

    /**
     * 发送错误日志
     * @param color 颜色
     * @param format 格式化字符串
     * @param args 参数
     */
    void error(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    /**
     * 创建一个定向到指定对象的 {@link Message}：消息只发给它，不写控制台，也不带级别标签。
     * 返回的实例可以反复使用。
     *
     * @param sender 接收日志的对象
     * @return 定向消息实例
     */
    Message to(@NotNull CommandSender sender);

    /**
     * 创建一个定向到指定对象的 Logger：带级别标签，同样只发给它、不写控制台。
     * 返回的实例可以反复使用。
     * <p>
     * 实际发出的形状是 {@code 插件前缀§8[<级别色>§l§n级别§8]§r 消息}：级别名加粗下划线，
     * 两侧方括号都是深灰色，级别色取该条日志的颜色（{@code info} 默认绿、{@code warn} 默认黄、
     * {@code error} 默认红、{@code debug} 默认蓝）。例如默认色的 {@code info("操作成功")} 发出去是
     * {@code MyPlugin§8[§a§l§nINFO§8]§r 操作成功}。
     *
     * @param sender 接收日志的对象
     * @return 定向 Logger 实例
     */
    Logger toLog(@NotNull CommandSender sender);
}
