package cn.yvmou.ylib.folia;

import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.common.YLibImpl;
import cn.yvmou.ylib.folia.scheduler.FoliaScheduler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YLib Folia实现类 - 提供Folia特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibImplFolia extends YLibImpl {
    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibImplFolia(JavaPlugin plugin) {
        super(plugin);
    }

    /**
     * 初始化Folia特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Folia特定的调度器管理器
            this.universalScheduler = new FoliaScheduler(plugin);

            LOGGER.info("Folia特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Folia特定组件初始化失败", e);
        }
    }
}