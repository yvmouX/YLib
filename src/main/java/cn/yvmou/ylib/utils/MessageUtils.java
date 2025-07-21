package cn.yvmou.ylib.utils;

import cn.yvmou.ylib.YLib;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;

public class MessageUtils {
    public static String format(String format, Object... args) {
        return String.format(format, args);
    }

    public static String oldColor(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 带前缀旧颜色
     *
     * @param prefix  前缀，可以为null。为null时自动从plugin.yml获取插件名称作为前缀
     * @param message 消息
     * @return {@link String }
     */
    public static String oldColorWithPrefix(@Nullable String prefix, String message) {
        if (prefix == null) {
            prefix = YLib.getyLib().getPrefix();
        }
        return ChatColor.translateAlternateColorCodes('&', "§8[§b§l§n" + prefix + "§8]§r " + message);
    }
}
