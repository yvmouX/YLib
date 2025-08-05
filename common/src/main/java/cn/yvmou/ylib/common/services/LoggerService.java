package cn.yvmou.ylib.common.services;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 日志服务类 - 提供统一的日志功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class LoggerService implements cn.yvmou.ylib.api.services.LoggerService {
    
    private final Plugin plugin;
    private final Logger logger;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public LoggerService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 记录信息日志
     * @param message 日志消息
     */
    public void info(@NotNull String message) {
        logger.info(message);
    }
    
    /**
     * 记录警告日志
     * @param message 日志消息
     */
    public void warn(@NotNull String message) {
        logger.warning(message);
    }
    
    /**
     * 记录错误日志
     * @param message 日志消息
     */
    public void error(@NotNull String message) {
        logger.severe(message);
    }
    
    /**
     * 记录错误日志
     * @param message 日志消息
     * @param throwable 异常
     */
    public void error(@NotNull String message, @NotNull Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
    
    /**
     * 记录调试日志
     * @param message 日志消息
     */
    public void debug(@NotNull String message) {
        logger.fine(message);
    }
    
    /**
     * 记录启动日志
     * @param message 日志消息
     */
    public void startup(@NotNull String message) {
        info("§a[STARTUP] " + message);
    }
    
    /**
     * 记录关闭日志
     * @param message 日志消息
     */
    public void shutdown(@NotNull String message) {
        info("§c[SHUTDOWN] " + message);
    }
    
    /**
     * 记录配置日志
     * @param message 日志消息
     */
    public void config(@NotNull String message) {
        info("§e[CONFIG] " + message);
    }
    
    /**
     * 记录命令日志
     * @param message 日志消息
     */
    public void command(@NotNull String message) {
        info("§6[COMMAND] " + message);
    }
    
    /**
     * 记录监听器日志
     * @param message 日志消息
     */
    public void listener(@NotNull String message) {
        info("§b[LISTENER] " + message);
    }
    
    /**
     * 记录性能日志
     * @param message 日志消息
     */
    public void performance(@NotNull String message) {
        info("§d[PERFORMANCE] " + message);
    }
    
    /**
     * 检查是否为调试模式
     * @return boolean 如果是调试模式返回true
     */
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }
    
    /**
     * 设置调试模式
     * @param enabled 是否启用调试模式
     */
    public void setDebugEnabled(boolean enabled) {
        if (enabled) {
            logger.setLevel(Level.FINE);
        } else {
            logger.setLevel(Level.INFO);
        }
    }
    
    /**
     * 记录警告日志（别名）
     * @param message 日志消息
     */
    @Override
    public void warning(@NotNull String message) {
        logger.warning(message);
    }
    
    /**
     * 记录严重错误日志
     * @param message 日志消息
     */
    @Override
    public void severe(@NotNull String message) {
        logger.severe(message);
    }
} 