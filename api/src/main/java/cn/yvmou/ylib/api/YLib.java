package cn.yvmou.ylib.api;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.error.YLibErrorHandler;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.services.ConfigurationService;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.api.services.MessageService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * YLib主要API接口，提供对所有YLib功能的访问
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface YLib {
    /**
     * 获取调度器管理器
     * 
     * @return 调度器管理器实例
     */
    @NotNull
    UniversalScheduler getScheduler();
    
    /**
     * 获取命令管理器
     * 
     * @return 命令管理器实例
     */
    @NotNull
    CommandManager getCommandManager();
    
    /**
     * 获取配置服务
     * 
     * @return 配置服务实例
     */
    @NotNull
    ConfigurationService getConfiguration();
    
    /**
     * 获取日志服务
     * 
     * @return 日志服务实例
     */
    @NotNull
    LoggerService getSimpleLogger();
    
    /**
     * 获取消息服务
     * 
     * @return 消息服务实例
     */
    @NotNull
    MessageService getMessages();
    
    /**
     * 获取插件实例
     * 
     * @return 插件实例
     */
    @NotNull
    JavaPlugin getPlugin();
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称
     */
    @NotNull
    String getPluginName();
    
    /**
     * 获取插件版本
     * 
     * @return 插件版本
     */
    @NotNull
    String getPluginVersion();
    
    /**
     * 获取插件前缀
     * 
     * @return 插件前缀
     */
    @NotNull
    String getPluginPrefix();
    
    /**
     * 获取YLib版本
     * 
     * @return YLib版本
     */
    @NotNull
    String getYLibVersion();
    
    /**
     * 获取服务器类型
     * 
     * @return 服务器类型
     */
    @NotNull
    String getServerType();
    
    /**
     * 检查是否为Folia服务器
     * 
     * @return 如果是Folia服务器返回true，否则返回false
     */
    boolean isFolia();
    
    /**
     * 检查是否为Paper服务器（包括Folia）
     * 
     * @return 如果是Paper或Folia服务器返回true，否则返回false
     */
    boolean isPaper();
    
    /**
     * 检查是否为Spigot服务器（包括Paper和Folia）
     * 
     * @return 如果是Spigot、Paper或Folia服务器返回true，否则返回false
     */
    boolean isSpigot();
    
    /**
     * 获取服务器版本信息
     * 
     * @return 服务器版本信息
     */
    @NotNull
    String getServerVersion();
    
    /**
     * 获取Bukkit版本信息
     * 
     * @return Bukkit版本信息
     */
    @NotNull
    String getBukkitVersion();
    
    /**
     * 获取配置管理器
     * <p>
     * 配置管理器提供基于注解的自动配置功能，支持约定优于配置的理念。
     * 让配置管理变得更加简单和智能。
     * </p>
     * 
     * <p>主要功能：</p>
     * <ul>
     *   <li>自动扫描和加载配置类</li>
     *   <li>自动生成默认配置文件</li>
     *   <li>配置值验证和类型转换</li>
     *   <li>配置热重载</li>
     *   <li>配置变更监听</li>
     * </ul>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * // 注册配置类
     * DatabaseConfig dbConfig = ylib.getConfigurationManager().registerConfiguration(DatabaseConfig.class);
     * 
     * // 使用配置
     * String host = dbConfig.getHost();
     * int port = dbConfig.getPort();
     * 
     * // 监听配置变更
     * ylib.getConfigurationManager().addConfigurationListener(DatabaseConfig.class, (oldConfig, newConfig) -> {
     *     // 重新连接数据库
     *     reconnectDatabase(newConfig);
     * });
     * }</pre>
     * 
     * @return 配置管理器实例
     */
    @NotNull
    ConfigurationManager getConfigurationManager();
    
    /**
     * 获取错误处理器
     * <p>
     * 错误处理器提供统一的错误处理机制，包括用户友好的错误消息生成、
     * 错误恢复建议、自动恢复尝试等功能。
     * </p>
     * 
     * <p>主要功能：</p>
     * <ul>
     *   <li>生成用户友好的错误消息</li>
     *   <li>提供错误恢复建议</li>
     *   <li>尝试自动错误恢复</li>
     *   <li>错误统计和监听</li>
     *   <li>详细的错误上下文记录</li>
     * </ul>
     * 
     * <p>使用示例：</p>
     * <pre>{@code
     * try {
     *     // 可能出错的操作
     *     riskyOperation();
     * } catch (Exception e) {
     *     ErrorContext context = new ErrorContext("DatabaseService", "connect", getPluginName());
     *     context.addContext("host", "localhost").addContext("port", 3306);
     *     
     *     YLibErrorHandler.ErrorHandlingResult result = ylib.getErrorHandler().handleError(e, context);
     *     
     *     // 显示用户友好消息
     *     getLogger().info(result.getUserMessage());
     *     
     *     // 显示恢复建议
     *     for (String suggestion : result.getSuggestions()) {
     *         getLogger().info("建议: " + suggestion);
     *     }
     * }
     * }</pre>
     * 
     * @return 错误处理器实例
     */
    @NotNull
    YLibErrorHandler getErrorHandler();
}