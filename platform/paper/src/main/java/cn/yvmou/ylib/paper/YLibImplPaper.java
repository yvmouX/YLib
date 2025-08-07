package cn.yvmou.ylib.paper;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.core.coreImpl.YLibImpl;
import cn.yvmou.ylib.paper.command.PaperCommandManager;
import cn.yvmou.ylib.paper.scheduler.PaperScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib Paper实现类 - 提供Paper特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibImplPaper extends YLibImpl {

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibImplPaper(@NotNull JavaPlugin plugin) {
        super(plugin);
    }

    /**
     * 初始化Paper特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Paper特定的调度器管理器
            this.universalScheduler = new PaperScheduler(plugin);

            // 初始化Paper特定的命令管理器
            this.commandManager = new PaperCommandManager(plugin);

            LOGGER.info("Paper特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Paper特定组件初始化失败", e);
        }
    }
} 