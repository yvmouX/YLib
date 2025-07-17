package cn.yvmou.ylib;

import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;

/**
 * 更好的JavaPlugin
 */
public class JavaPluginR extends JavaPlugin {
    private static JavaPluginR instance;

    /**
     * 构造函数
     */
    public JavaPluginR() {
        super();
        instance = this;
    }

    /**
     * 获取此插件的实例。
     *
     * @return 此插件的实例。
     * @throws IllegalStateException 当该插件尚未加载时。
     */
    @Nonnull
    public static JavaPluginR getInstance() {
        if (instance == null) {
            throw new IllegalStateException("This plugin hasn't been loaded yet!");
        }
        return instance;
    }

    /**
     *
     * 用于全局持有YLib实例，便于工具类访问。
     */
    public static class YLibHolder {
        private static YLib yLib;

        private YLibHolder() {}

        /**
         * 1
         * @param yLibInstance 1
         */
        public static void setYLib(YLib yLibInstance) {
            yLib = yLibInstance;
        }

        /**
         * 1
         * @return 1
         */
        public static YLib getYLib() {
            if (yLib == null) throw new IllegalStateException("YLib尚未初始化");
            return yLib;
        }
    }
}
