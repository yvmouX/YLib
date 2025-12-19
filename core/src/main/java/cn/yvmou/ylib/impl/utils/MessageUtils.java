package cn.yvmou.ylib.impl.utils;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class MessageUtils {
    public static String format(String format, Object... args) {
        return String.format(format, args);
    }

    public static String oldColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String oldColorWithPrefix(@NotNull String prefix, String message) {
        return ChatColor.translateAlternateColorCodes('&', "§8[§b§l§n" + prefix + "§8]§r " + message);
    }
}
