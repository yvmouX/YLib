package cn.yvmou.ylib.common.services;

import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.api.services.LoggerService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 配置服务类 - 提供配置管理功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public class ConfigurationService implements cn.yvmou.ylib.api.services.ConfigurationService {
    
    private final Plugin plugin;
    private final LoggerService logger;
    private FileConfiguration config;
    private File configFile;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param logger 日志服务
     */
    public ConfigurationService(@NotNull Plugin plugin, @NotNull LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        initializeConfig();
    }
    
    /**
     * 保存默认配置
     */
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            logger.warn("Default configuration saved");
        }
    }
    
    /**
     * 获取配置
     * @return FileConfiguration 配置对象
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
        logger.warn("Configuration reloaded");
    }
    
    /**
     * 保存配置
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            logger.warn("Configuration saved");
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
            throw new YLibException("Failed to save configuration", e);
        }
    }
    
    /**
     * 初始化配置
     */
    private void initializeConfig() {
        saveDefaultConfig();
        loadConfig();
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 加载默认配置
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)
            );
            config.setDefaults(defConfig);
        }
    }
    
    /**
     * 获取自定义配置文件
     * @param fileName 文件名
     * @return FileConfiguration 配置对象
     */
    public FileConfiguration getCustomConfig(@NotNull String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    /**
     * 保存自定义配置文件
     * @param fileName 文件名
     * @param config 配置对象
     */
    public void saveCustomConfig(@NotNull String fileName, @NotNull FileConfiguration config) {
        try {
            File file = new File(plugin.getDataFolder(), fileName);
            config.save(file);
            logger.warn("Custom configuration saved: " + fileName);
        } catch (IOException e) {
            logger.error("Failed to save custom configuration: " + fileName, e);
            throw new YLibException("Failed to save custom configuration: " + fileName, e);
        }
    }
    
    /**
     * 获取字符串值
     * @param path 配置路径
     * @return 字符串值，如果不存在则返回null
     */
    @Override
    @Nullable
    public String getString(@NotNull String path) {
        return config.getString(path);
    }
    
    /**
     * 获取字符串配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return String 配置值
     */
    @Override
    @NotNull
    public String getString(@NotNull String path, @NotNull String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * 获取整数值
     * @param path 配置路径
     * @return int 整数值
     */
    @Override
    public int getInt(@NotNull String path) {
        return config.getInt(path);
    }
    
    /**
     * 获取整数配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return int 配置值
     */
    @Override
    public int getInt(@NotNull String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * 获取布尔值
     * @param path 配置路径
     * @return boolean 布尔值
     */
    @Override
    public boolean getBoolean(@NotNull String path) {
        return config.getBoolean(path);
    }
    
    /**
     * 获取布尔配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return boolean 配置值
     */
    @Override
    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * 获取双精度浮点数配置
     * @param path 配置路径
     * @param defaultValue 默认值
     * @return double 配置值
     */
    public double getDouble(@NotNull String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    /**
     * 设置配置值
     * @param path 配置路径
     * @param value 配置值
     */
    public void set(@NotNull String path, Object value) {
        config.set(path, value);
    }
    
    /**
     * 检查配置路径是否存在
     * @param path 配置路径
     * @return boolean 如果路径存在返回true
     */
    public boolean contains(@NotNull String path) {
        return config.contains(path);
    }
    
    /**
     * 获取字符串列表
     * @param path 配置路径
     * @return 字符串列表，如果不存在则返回null
     */
    @Override
    @Nullable
    public List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }
    
    /**
     * 获取双精度浮点数值
     * @param path 配置路径
     * @return 双精度浮点数值
     */
    @Override
    public double getDouble(@NotNull String path) {
        return config.getDouble(path);
    }
} 