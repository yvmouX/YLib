package cn.yvmou.ylib.spigot;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.core.coreImpl.YLibImpl;
import cn.yvmou.ylib.spigot.command.SpigotCommandManager;
import cn.yvmou.ylib.spigot.scheduler.SpigotScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib Spigot实现类 - 提供Spigot特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibImplSpigot extends YLibImpl {

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibImplSpigot(@NotNull JavaPlugin plugin) {
        super(plugin);
    }

    /**
     * 初始化Spigot特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Spigot特定的调度器管理器
            this.universalScheduler = new SpigotScheduler(plugin);

            // 初始化Spigot特定的命令管理器
            this.commandManager = new SpigotCommandManager(plugin);

            LOGGER.info("Spigot特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Spigot特定组件初始化失败", e);
        }
    }
} 