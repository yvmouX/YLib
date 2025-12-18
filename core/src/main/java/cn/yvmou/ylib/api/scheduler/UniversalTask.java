package cn.yvmou.ylib.api.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * 任务接口 - 定义任务的基本操作
 *
 * @author yvmoux
 * @since 1.0.0
 */
public interface UniversalTask {

    /**
     * 获取任务所属插件
     * @return Plugin 任务所属插件
     */
    Plugin getOwningPlugin();

    /**
     * 取消任务
     */
    void cancel();

    /**
     * 检查任务是否已取消
     * @return boolean 如果任务已取消返回true
     */
    boolean isCancelled();

    /**
     * 检查任务是否正在运行
     * @return boolean 如果任务正在运行返回true
     */
    boolean isCurrentlyRunning();

    /**
     * 获取任务类型
     * @return TaskType 任务类型
     */
    TaskType getType();

    /**
     * 任务类型枚举
     */
    enum TaskType {
        /** 同步任务 */
        SYNC,
        /** 异步任务 */
        ASYNC,
        /** 重复任务 */
        REPEATING
    }
}