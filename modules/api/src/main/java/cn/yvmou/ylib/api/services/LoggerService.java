package cn.yvmou.ylib.api.services;

import org.jetbrains.annotations.NotNull;

/**
 * 日志服务接口
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface LoggerService {
    
    /**
     * 记录信息日志
     * 
     * @param message 日志消息
     */
    void info(@NotNull String message);
    
    /**
     * 记录警告日志
     * 
     * @param message 日志消息
     */
    void warn(@NotNull String message);
    
    /**
     * 记录错误日志
     * 
     * @param message 日志消息
     */
    void error(@NotNull String message);
    
    /**
     * 记录错误日志（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    void error(@NotNull String message, @NotNull Throwable throwable);
    
    /**
     * 记录调试日志
     * 
     * @param message 日志消息
     */
    void debug(@NotNull String message);
    
    /**
     * 记录启动日志
     * 
     * @param message 日志消息
     */
    void startup(@NotNull String message);
    
    /**
     * 记录关闭日志
     * 
     * @param message 日志消息
     */
    void shutdown(@NotNull String message);
    
    /**
     * 记录配置日志
     * 
     * @param message 日志消息
     */
    void config(@NotNull String message);
    
    /**
     * 记录命令日志
     * 
     * @param message 日志消息
     */
    void command(@NotNull String message);
    
    /**
     * 记录监听器日志
     * 
     * @param message 日志消息
     */
    void listener(@NotNull String message);
    
    /**
     * 记录性能日志
     * 
     * @param message 日志消息
     */
    void performance(@NotNull String message);
    
    /**
     * 记录严重错误日志
     * 
     * @param message 日志消息
     */
    void severe(@NotNull String message);
    
    /**
     * 记录警告日志（别名）
     * 
     * @param message 日志消息
     */
    void warning(@NotNull String message);
}