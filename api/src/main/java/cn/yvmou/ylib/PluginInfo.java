package cn.yvmou.ylib;

public class PluginInfo {
    private static String pluginName = null;
    private static String pluginPrefix = null;
    private static String pluginVersion = null;
    // 日志
    private static boolean loggerDebug = true;
    private static String loggerPrefix = null;

    // 以下 setter 仅供 YLib 初始化时写入，外部代码不应直接修改
    static void setPluginName(String name) {
        pluginName = name;
    }

    static void setPluginPrefix(String prefix) {
        pluginPrefix = prefix;
    }

    static void setPluginVersion(String version) {
        pluginVersion = version;
    }

    static void setLoggerPrefix(String prefix) {
        loggerPrefix = prefix;
    }

    public static void setLoggerDebug(boolean debug) {
        loggerDebug = debug;
    }

    public static String getPluginName() {
        return pluginName;
    }

    public static String getPluginPrefix() {
        return pluginPrefix;
    }

    public static String getPluginVersion() {
        return pluginVersion;
    }

    public static boolean getLoggerDebug() {
        return loggerDebug;
    }

    public static String getLoggerPrefix() {
        return loggerPrefix;
    }
}
