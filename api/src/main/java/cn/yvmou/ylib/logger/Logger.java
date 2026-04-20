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
     * 创建一个临时的 Logger，将下一条日志发送给指定对象
     * @param sender 接收日志的对象
     * @return 临时的 Logger 实例
     */
    Logger to(@NotNull CommandSender sender);
}
