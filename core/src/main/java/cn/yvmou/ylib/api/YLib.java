package cn.yvmou.ylib.api;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.scheduler.UniversalScheduler;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.api.services.ServerInfoService;
import cn.yvmou.ylib.enums.LoggerOption;
import cn.yvmou.ylib.impl.logger.LoggerImpl;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public interface YLib {
    @NotNull
    UniversalScheduler getScheduler();
    /**
     * 获取服务器信息服务
     *
     * @return 服务器信息服务实例
     */
    @NotNull ServerInfoService getServerInfo();

    /**
     * 获取命令管理器
     *
     * @return 命令管理器实例
     */
    @NotNull
    CommandManager getCommandManager();


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


    LoggerImpl createLogger();

    LoggerImpl createLogger(@NotNull LoggerOption option);

    LoggerImpl createLogger(@NotNull String prefix);

    LoggerImpl createLogger(@NotNull String prefix, @NotNull LoggerOption option);

    String getConsolePrefix();
}
