package cn.yvmou.ylib.api.config;

import cn.yvmou.ylib.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 配置管理器接口
 * <p>
 * 提供自动配置和约定优于配置的功能，让配置管理变得更加简单和智能。
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
 * configManager.registerConfiguration(DatabaseConfig.class);
 * 
 * // 获取配置实例
 * DatabaseConfig dbConfig = configManager.getConfiguration(DatabaseConfig.class);
 * 
 * // 监听配置变更
 * configManager.addConfigurationListener(DatabaseConfig.class, (oldConfig, newConfig) -> {
 *     // 处理配置变更
 * });
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface ConfigurationManager {
    
    /**
     * 注册配置类
     * <p>
     * 扫描配置类的注解信息，自动加载配置值，如果需要则生成默认配置文件。
     * </p>
     * 
     * @param <T> 配置类型
     * @param configClass 配置类
     * @return 配置实例
     * @throws ConfigurationException 如果注册失败
     */
    @NotNull
    <T> T registerConfiguration(@NotNull Class<T> configClass) throws ConfigurationException;
    
    /**
     * 获取配置实例
     * 
     * @param <T> 配置类型
     * @param configClass 配置类
     * @return 配置实例，如果未注册则返回null
     */
    @Nullable
    <T> T getConfiguration(@NotNull Class<T> configClass);
    
    /**
     * 获取必需的配置实例
     * 
     * @param <T> 配置类型
     * @param configClass 配置类
     * @return 配置实例
     * @throws ConfigurationException 如果配置未注册
     */
    @NotNull
    <T> T getRequiredConfiguration(@NotNull Class<T> configClass) throws ConfigurationException;
    
    /**
     * 重新加载配置
     * <p>
     * 从配置文件重新读取值并更新配置实例。
     * </p>
     * 
     * @param configClass 配置类
     * @return 如果重载成功返回true，否则返回false
     */
    boolean reloadConfiguration(@NotNull Class<?> configClass);
    
    /**
     * 重新加载所有配置
     * 
     * @return 成功重载的配置数量
     */
    int reloadAllConfigurations();
    
    /**
     * 保存配置到文件
     * <p>
     * 将当前配置实例的值保存到配置文件中。
     * </p>
     * 
     * @param configClass 配置类
     * @return 如果保存成功返回true，否则返回false
     */
    boolean saveConfiguration(@NotNull Class<?> configClass);
    
    /**
     * 验证配置
     * <p>
     * 检查配置值是否符合验证规则。
     * </p>
     * 
     * @param configClass 配置类
     * @return 验证结果
     */
    @NotNull
    ConfigurationValidationResult validateConfiguration(@NotNull Class<?> configClass);
    
    /**
     * 添加配置变更监听器
     * 
     * @param <T> 配置类型
     * @param configClass 配置类
     * @param listener 变更监听器
     */
    <T> void addConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener);
    
    /**
     * 移除配置变更监听器
     * 
     * @param <T> 配置类型
     * @param configClass 配置类
     * @param listener 变更监听器
     */
    <T> void removeConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener);
    
    /**
     * 获取所有已注册的配置类
     * 
     * @return 已注册的配置类列表
     */
    @NotNull
    List<Class<?>> getRegisteredConfigurations();
    
    /**
     * 获取配置统计信息
     * 
     * @return 配置统计信息
     */
    @NotNull
    Map<String, Object> getStatistics();
    
    /**
     * 配置变更监听器
     * 
     * @param <T> 配置类型
     */
    @FunctionalInterface
    interface ConfigurationChangeListener<T> {
        /**
         * 配置变更时调用
         * 
         * @param oldConfiguration 旧配置
         * @param newConfiguration 新配置
         */
        void onConfigurationChanged(@Nullable T oldConfiguration, @NotNull T newConfiguration);
    }
}