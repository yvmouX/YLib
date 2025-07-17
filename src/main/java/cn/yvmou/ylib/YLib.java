package cn.yvmou.ylib;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.plugin.Plugin;

/**
 * YLib 是一个 Minecraft 插件的主类，提供插件的核心功能和工具。
 * 它扩展了 Bukkit 的 JavaPlugin 类，并集成了 FoliaLib 以支持 Folia 服务器。
 */
public final class YLib {
    private final Plugin plugin;
    private final FoliaLib foliaLib;

    /**
     * 实例化。
     *
     * @param plugin 插件
     */
    public YLib(Plugin plugin) {
        foliaLib = new FoliaLib(plugin);
        this.plugin = plugin;
        // 注册全局YLib实例
        JavaPluginR.YLibHolder.setYLib(this);
    }

    /**
     * 获取 FoliaLib 实例。
     *
     * @return FoliaLib 的实例。
     */
    public FoliaLib getFoliaLib() { return foliaLib; }

    /**
     * 获取 Plugin 实例
     *
     * @return Plugin
     */
    public Plugin getPlugin() { return plugin; }
}
