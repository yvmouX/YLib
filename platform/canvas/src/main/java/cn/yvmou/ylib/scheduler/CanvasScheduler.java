package cn.yvmou.ylib.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * Canvas 调度器管理器实现
 * <p>
 * Canvas 是 Paper 的 fork（重新实现了 Folia 的区域多线程模型），
 * 插件侧调度 API 与 Folia 完全一致，直接复用 Folia 调度实现。
 * 服务端内部若启用 Affinity 调度器（canvas 推荐配置），提交的任务自动受益。
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class CanvasScheduler extends FoliaScheduler {

    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public CanvasScheduler(Plugin plugin) {
        super(plugin);
    }
}
