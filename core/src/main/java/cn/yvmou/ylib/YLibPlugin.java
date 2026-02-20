package cn.yvmou.ylib;

import cn.yvmou.ylib.exception.YLibException;
import org.bukkit.plugin.java.JavaPlugin;

public class YLibPlugin extends JavaPlugin {

    private static YLib instance;

    @Override
    public void onEnable() {
        try {
            // 初始化 YLib
            instance = new YLib(this);
            
            // 日志输出
            getLogger().info("YLib has been loaded successfully!");
            getLogger().info("Version: " + getDescription().getVersion());
            getLogger().info("Server Type: " + instance.getScheduler().getClass().getSimpleName().replace("Scheduler", ""));
            
        } catch (YLibException e) {
            getLogger().severe("Failed to initialize YLib!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // YLib 不需要显式的销毁步骤，它的组件会随插件禁用而停止
        instance = null;
        getLogger().info("YLib has been disabled.");
    }

    /**
     * 获取 YLib 实例
     * 当其他插件作为依赖使用 YLib 时，可以通过此方法获取共享实例
     * @return YLib 实例
     */
    public static YLib getInstance() {
        return instance;
    }
}
