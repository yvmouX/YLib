package cn.yvmou.ylib.api.services;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 配置服务接口
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface ConfigurationService {
    
    /**
     * 获取配置文件
     * 
     * @return 配置文件实例
     */
    @NotNull
    FileConfiguration getConfig();
    
    /**
     * 重载配置
     */
    void reloadConfig();
    
    /**
     * 保存配置
     */
    void saveConfig();
    
    /**
     * 获取字符串值
     * 
     * @param path 配置路径
     * @return 字符串值，如果不存在则返回null
     */
    @Nullable
    String getString(@NotNull String path);
    
    /**
     * 获取字符串值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 字符串值，如果不存在则返回默认值
     */
    @NotNull
    String getString(@NotNull String path, @NotNull String defaultValue);
    
    /**
     * 获取整数值
     * 
     * @param path 配置路径
     * @return 整数值
     */
    int getInt(@NotNull String path);
    
    /**
     * 获取整数值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 整数值，如果不存在则返回默认值
     */
    int getInt(@NotNull String path, int defaultValue);
    
    /**
     * 获取布尔值
     * 
     * @param path 配置路径
     * @return 布尔值
     */
    boolean getBoolean(@NotNull String path);
    
    /**
     * 获取布尔值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 布尔值，如果不存在则返回默认值
     */
    boolean getBoolean(@NotNull String path, boolean defaultValue);
    
    /**
     * 获取双精度浮点数值
     * 
     * @param path 配置路径
     * @return 双精度浮点数值
     */
    double getDouble(@NotNull String path);
    
    /**
     * 获取双精度浮点数值
     * 
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return 双精度浮点数值，如果不存在则返回默认值
     */
    double getDouble(@NotNull String path, double defaultValue);
    
    /**
     * 获取字符串列表
     * 
     * @param path 配置路径
     * @return 字符串列表，如果不存在则返回null
     */
    @Nullable
    List<String> getStringList(@NotNull String path);
    
    /**
     * 设置配置值
     * 
     * @param path 配置路径
     * @param value 配置值
     */
    void set(@NotNull String path, @Nullable Object value);
    
    /**
     * 检查配置路径是否存在
     * 
     * @param path 配置路径
     * @return 如果存在返回true，否则返回false
     */
    boolean contains(@NotNull String path);
}