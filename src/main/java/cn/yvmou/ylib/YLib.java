package cn.yvmou.ylib;

import com.tcoded.folialib.FoliaLib;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YLib 是一个 Minecraft 插件的主类，提供插件的核心功能和工具。
 * 它扩展了 Bukkit 的 JavaPlugin 类，并集成了 FoliaLib 以支持 Folia 服务器。
 */
public final class YLib extends JavaPlugin {
    private static YLib instance;
    private static FoliaLib foliaLib;

    /**
     * 私有构造函数，防止实例化。
     */
    private YLib() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * 获取插件实例。
     *
     * @return 当前插件的实例。
     */
    public static YLib getInstance() { return instance; }
    /**
     * 获取 FoliaLib 实例。
     *
     * @return FoliaLib 的实例。
     */
    public static FoliaLib getFoliaLib() { return foliaLib; }

    /**
     * 在插件加载时初始化插件实例和 FoliaLib。
     */
    @Override
    public void onLoad() {
        instance = this;
        foliaLib = new FoliaLib(this);
    }
}
