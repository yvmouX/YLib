package cn.yvmou.ylib.paper;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.core.AbstractYLib;
import cn.yvmou.ylib.paper.command.PaperCommandManager;
import cn.yvmou.ylib.paper.scheduler.PaperSchedulerManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib Paper实现类 - 提供Paper特定的功能实现
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibPaper extends AbstractYLib {
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @throws YLibException 如果初始化失败
     */
    public YLibPaper(@NotNull JavaPlugin plugin) throws YLibException {
        super(plugin, ServerType.PAPER);
        
        // 验证不是Folia服务器
        if (ServerType.FOLIA.selfCheck()) {
            throw new YLibException("YLibPaper不能在Folia服务器上使用，请使用YLibFolia");
        }
    }
    
    /**
     * 初始化Paper特定的组件
     */
    @Override
    protected void initializePlatformSpecific() throws YLibException {
        try {
            // 初始化Paper特定的调度器管理器
            this.schedulerManager = new PaperSchedulerManager(plugin);
            
            // 初始化Paper特定的命令管理器
            this.commandManager = new PaperCommandManager(plugin);
            
            getLogger().info("Paper特定组件初始化完成");
        } catch (Exception e) {
            throw new YLibException("Paper特定组件初始化失败", e);
        }
    }
    
    /**
     * Paper启用时的特定逻辑
     */
    @Override
    protected void onEnable() {
        super.onEnable();
        getLogger().info("Paper增强调度器已启用");
        getLogger().info("支持Paper特有的异步功能");
    }
    
    /**
     * Paper禁用时的特定逻辑
     */
    @Override
    protected void onDisable() {
        super.onDisable();
        getLogger().info("Paper特定功能已清理");
    }
} 