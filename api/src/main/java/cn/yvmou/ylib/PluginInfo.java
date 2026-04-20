package cn.yvmou.ylib;

public class PluginInfo {
    public static String pluginName = null;
    public static String pluginPrefix = null;
    public static String pluginVersion = null;
    // 日志
    public static boolean loggerDebug = true;
    public static String loggerPrefix = null;

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
