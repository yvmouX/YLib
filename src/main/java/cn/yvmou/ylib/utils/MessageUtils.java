package cn.yvmou.ylib.utils;

import cn.yvmou.ylib.YlibR;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtils {
    private MessageUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    private static String getConsolePrefix() {
        try {
            return "§8[§b§l§n" + YlibR.getInstance().getDescription().getPrefix() + "§8]§r ";
        } catch (Exception e) {
            return "§8[§b§l§nYLib§r] ";
        }
    }

    public static void info(Player player, String msg) {
        info(player, ChatColor.GREEN, msg); // 默认使用绿色
    }

    /**
     * color info
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void info(Player player, ChatColor color, String msg) {
        player.sendMessage(getConsolePrefix() + (color == null ? ChatColor.GREEN : color) + msg);
    }

    /**
     * warn
     *
     * @param msg 颜色
     */
    public static void warn(Player player, String msg) {
        warn(player, ChatColor.GOLD, msg);
    }

    /**
     * color warn
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void warn(Player player, ChatColor color, String msg) {
        player.sendMessage(getConsolePrefix() + (color == null ? ChatColor.GOLD : color) + msg);
    }

    /**
     * err
     *
     * @param msg 消息
     */
    public static void error(Player player, String msg) {
        error(player, ChatColor.RED, msg);
    }

    /**
     * color err
     *
     * @param color 颜色
     * @param msg 消息
     */
    public static void error(Player player, ChatColor color, String msg) {
        player.sendMessage(getConsolePrefix() + (color == null ? ChatColor.RED : color) + msg);
    }
}
