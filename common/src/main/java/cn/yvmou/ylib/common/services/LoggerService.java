package cn.yvmou.ylib.common.services;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 日志服务类 - 提供统一的日志功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class LoggerService implements cn.yvmou.ylib.api.services.LoggerService {
    private final Plugin plugin;

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public LoggerService(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getConsolePrefix() {
        try {
            return "§8[§b§l§n" + plugin.getDescription().getName() + "§8]§r ";
        } catch (Exception e) {
            return "§8[§b§l§nYLib§r] ";
        }
    }

    /**
     * info
     *
     * @param msg 消息
     */
    public void info(@NotNull String msg) {
        info(ChatColor.GREEN, msg); // 默认使用绿色
    }

    /**
     * color info
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void info(ChatColor color, @NotNull String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GREEN : color) + msg);
    }

    /**
     * warn
     *
     * @param msg 颜色
     */
    public void warn(@NotNull String msg) {
        warn(ChatColor.GOLD, msg);
    }

    /**
     * color warn
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void warn(ChatColor color, @NotNull String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GOLD : color) + msg);
    }

    /**
     * err
     *
     * @param msg 消息
     */
    public void error(@NotNull String msg) {
        error(ChatColor.RED, msg);
    }

    /**
     * color err
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void error(ChatColor color, @NotNull String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.RED : color) + msg);
    }

    /**
     * error
     *
     * @param msg 颜色
     * @param throwable 消息
     */
    public void error(@NotNull String msg, @NotNull Throwable throwable) {
        error(msg); // 打印错误信息
        throwable.printStackTrace(); // 打印堆栈跟踪
    }
} 