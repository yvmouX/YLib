package cn.yvmou.ylib.api.services;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 日志服务接口
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface LoggerService {
    /**
     * 获取插件前缀
     * @return 插件前置
     */
    String getConsolePrefix();

    /**
     * 信息
     *
     * @param msg 日志消息
     */
    void info(@NotNull String msg);

    /**
     * 颜色信息
     *
     * @param color 颜色
     * @param msg 消息
     */
    void info(ChatColor color, @NotNull String msg);

    /**
     * 警告
     *
     * @param msg 颜色
     */
    void warn(@NotNull String msg);

    /**
     * 颜色警告
     *
     * @param color 颜色
     * @param msg 消息
     */
    void warn(ChatColor color, @NotNull String msg);

    /**
     * 错误
     *
     * @param msg 消息
     */
    void error(@NotNull String msg);

    /**
     * 颜色错误
     *
     * @param color 颜色
     * @param msg 消息
     */
    void error(ChatColor color, @NotNull String msg);

    /**
     * 异常错误
     *
     * @param msg 颜色
     * @param throwable 消息
     */
    void error(@NotNull String msg, @NotNull Throwable throwable);
}