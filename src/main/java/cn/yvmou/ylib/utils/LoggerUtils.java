package cn.yvmou.ylib.utils;

import org.bukkit.ChatColor;

/**
 * 消息工具类
 */
public class LoggerUtils {
    // 延迟获取插件前缀，避免静态初始化时访问实例方法
    private static String getConsolePrefix() {
        try {
            return "§8[§b§l§n" + cn.yvmou.ylib.JavaPluginR.getInstance().getDescription().getPrefix() + "]§r ";
        } catch (Exception e) {
            return "§8[§b§l§nYLib§r] ";
        }
    }

    /**
     * 私有构造函数，防止实例化。
     */
    private LoggerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * info
     *
     * @param msg 消息
     */
    public static void info(String msg) {
        info(ChatColor.GREEN, msg); // 默认使用绿色
    }

    /**
     * color info
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void info(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GREEN : color) + msg);
    }

    /**
     * warn
     *
     * @param msg 颜色
     */
    public static void warn(String msg) {
        warn(ChatColor.GOLD, msg);
    }

    /**
     * color warn
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void warn(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.GOLD : color) + msg);
    }

    /**
     * err
     *
     * @param msg 消息
     */
    public static void error(String msg) {
        error(ChatColor.RED, msg);
    }

    /**
     * color err
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void error(ChatColor color, String msg) {
        org.bukkit.Bukkit.getConsoleSender().sendMessage(getConsolePrefix() + (color == null ? ChatColor.RED : color) + msg);
    }

    /**
     * error
     *
     * @param msg 颜色
     * @param throwable 消息
     */
    public static void error(String msg, Throwable throwable) {
        error(msg); // 打印错误信息
        if (throwable != null) {
            throwable.printStackTrace(); // 打印堆栈跟踪
        }
    }
}
