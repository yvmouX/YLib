package cn.yvmou.ylib.api.scheduler;

/**
 * 任务接口 - 定义任务的基本操作
 *
 * @author yvmoux
 * @since 1.0.0
 */
public interface Task {
    
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
     * 获取任务ID
     * @return int 任务ID
     */
    int getTaskId();
    
    /**
     * 检查任务是否正在运行
     * @return boolean 如果任务正在运行返回true
     */
    boolean isRunning();
    
    /**
     * 获取延迟时间
     * @return long 延迟时间（tick）
     */
    long getDelay();
    
    /**
     * 获取周期时间
     * @return long 周期时间（tick），如果不是重复任务返回-1
     */
    long getPeriod();
    
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
        /** 延迟任务 */
        DELAYED,
        /** 重复任务 */
        REPEATING
    }
} 