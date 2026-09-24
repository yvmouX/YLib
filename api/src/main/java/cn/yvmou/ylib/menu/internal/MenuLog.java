package cn.yvmou.ylib.menu.internal;

import org.bukkit.Bukkit;

import java.util.function.Consumer;

/**
 * 菜单框架自己的日志出口。
 * <p>
 * 与「抛异常」的分工：<b>代码写错 → 抛</b>（布局、符号、perPage 这类，是模板作者的问题，
 * 当场炸最省事）；<b>配置脏数据 → 记日志并继续</b>（服主写错一个材质名不该让整个菜单打不开）。
 * 这里只管后者。
 * <p>
 * 宿主可以接管出口（{@link #sink}）把警告转进自己的日志器；不接管就打控制台。
 */
public final class MenuLog {

    private static volatile Consumer<String> sink;

    private MenuLog() {
    }

    /** 接管日志出口（例如转给宿主自己的 Logger）。 */
    public static void sink(Consumer<String> consumer) {
        sink = consumer;
    }

    /** 当前出口；{@code null} 表示打控制台。 */
    public static Consumer<String> sink() {
        return sink;
    }

    public static void warn(String message) {
        Consumer<String> current = sink;
        if (current != null) {
            current.accept(message);
            return;
        }
        try {
            Bukkit.getLogger().warning("[YLib] 菜单: " + message);
        } catch (RuntimeException noServer) {
            // 单测里没有服务端：日志不该成为失败原因
        }
    }
}
