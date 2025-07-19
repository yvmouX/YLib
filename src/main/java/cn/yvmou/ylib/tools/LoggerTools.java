package cn.yvmou.ylib.tools;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

public class LoggerTools {
    private final Plugin plugin;

    public LoggerTools(Plugin plugin) {
        this.plugin = plugin;
    }

    // 延迟获取插件前缀，避免静态初始化时访问实例方法
    private String getConsolePrefix() {
        try {
            return "§8[§b§l§n" + plugin.getDescription().getPrefix() + "§8]§r ";
        } catch (Exception e) {
            return "§8[§b§l§nYLib§r] ";
        }
    }

    /**
     * info
     *
     * @param msg 消息
     */
    public void info(String msg) {
        info(ChatColor.GREEN, msg); // 默认使用绿色
    }

    /**
     * color info
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void info(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GREEN : color) + msg);
    }

    /**
     * warn
     *
     * @param msg 颜色
     */
    public void warn(String msg) {
        warn(ChatColor.GOLD, msg);
    }

    /**
     * color warn
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void warn(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GOLD : color) + msg);
    }

    /**
     * err
     *
     * @param msg 消息
     */
    public void error(String msg) {
        error(ChatColor.RED, msg);
    }

    /**
     * color err
     *
     * @param color 颜色
     * @param msg 消息
     */
    public void error(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.RED : color) + msg);
    }

    /**
     * error
     *
     * @param msg 颜色
     * @param throwable 消息
     */
    public void error(String msg, Throwable throwable) {
        error(msg); // 打印错误信息
        if (throwable != null) {
            throwable.printStackTrace(); // 打印堆栈跟踪
        }
    }
}
