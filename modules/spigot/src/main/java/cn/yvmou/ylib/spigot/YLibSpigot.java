package cn.yvmou.ylib.spigot;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.core.AbstractYLib;
import cn.yvmou.ylib.spigot.command.SpigotCommandManager;
import cn.yvmou.ylib.spigot.scheduler.SpigotSchedulerManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib Spigot实现类 - 提供Spigot特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibSpigot extends AbstractYLib {
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibSpigot(@NotNull JavaPlugin plugin) throws YLibException {
        super(plugin, ServerType.SPIGOT);
        
        // 验证不是Folia服务器
        if (ServerType.FOLIA.selfCheck()) {
            throw new YLibException("YLibSpigot不能在Folia服务器上使用，请使用YLibFolia");
        }
    }
    
    /**
     * 初始化Spigot特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Spigot特定的调度器管理器
            this.schedulerManager = new SpigotSchedulerManager(plugin);
            
            // 初始化Spigot特定的命令管理器
            this.commandManager = new SpigotCommandManager(plugin);
            
            getLogger().info("Spigot特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Spigot特定组件初始化失败", e);
        }
    }
    
    /**
     * Spigot启用时的特定逻辑
     */
    @Override
    protected void onEnable() {
        super.onEnable();
        getLogger().info("Spigot标准调度器已启用");
        getLogger().info("使用传统的全局调度器");
    }
    
    /**
     * Spigot禁用时的特定逻辑
     */
    @Override
    protected void onDisable() {
        super.onDisable();
        getLogger().info("Spigot特定功能已清理");
    }
} 