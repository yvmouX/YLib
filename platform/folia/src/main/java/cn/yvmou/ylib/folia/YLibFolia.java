package cn.yvmou.ylib.folia;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.core.AbstractYLib;
import cn.yvmou.ylib.folia.command.FoliaCommandManager;
import cn.yvmou.ylib.folia.scheduler.FoliaSchedulerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * YLib Folia实现类 - 提供Folia特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibFolia extends AbstractYLib {
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibFolia(JavaPlugin plugin) throws YLibException {
        super(plugin, ServerType.FOLIA);
        
        // 验证是否为Folia服务器
        if (!ServerType.FOLIA.selfCheck()) {
            throw new YLibException("YLibFolia需要Folia服务器环境");
        }
    }
    
    /**
     * 初始化Folia特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Folia特定的调度器管理器
            this.schedulerManager = new FoliaSchedulerManager(plugin);
            
            // 初始化Folia特定的命令管理器
            this.commandManager = new FoliaCommandManager(plugin);
            
            getLogger().info("Folia特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Folia特定组件初始化失败", e);
        }
    }
    
    /**
     * Folia启用时的特定逻辑
     */
    @Override
    protected void onEnable() {
        super.onEnable();
        getLogger().info("Folia区域化调度器已启用");
        getLogger().info("支持实体调度和位置调度");
    }
    
    /**
     * Folia禁用时的特定逻辑
     */
    @Override
    protected void onDisable() {
        super.onDisable();
        getLogger().info("Folia特定功能已清理");
    }
} 