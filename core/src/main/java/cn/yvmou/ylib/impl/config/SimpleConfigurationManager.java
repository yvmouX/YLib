package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.config.ConfigurationManager;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult.ValidationError;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.exception.ConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleConfigurationManager implements ConfigurationManager {
    private final LoggerService logger;
    
    private final ConfigurationParser parser;
    private final ConfigurationLoader loader;
    private final ConfigurationValidator validator;
    
    // 配置实例缓存 Map Class<?> -> ClassInstance
    private final Map<Class<?>, Object> configurationInstances = new ConcurrentHashMap<>();
    
    // 配置元数据缓存 Map Class<?> -> ConfigurationMetadata
    private final Map<Class<?>, ConfigurationMetadata> configurationMetadata = new ConcurrentHashMap<>();
    
    // 配置变更监听器
    private final Map<Class<?>, List<ConfigurationChangeListener<Object>>> listeners = new ConcurrentHashMap<>();
    
    // 统计信息
    private volatile int totalReloads = 0;
    private volatile int totalValidations = 0;

    public SimpleConfigurationManager(@NotNull JavaPlugin plugin, @NotNull LoggerService logger) {
        this.logger = logger;
        this.parser = new ConfigurationParser();
        this.loader = new ConfigurationLoader(plugin, logger);
        this.validator = new ConfigurationValidator();
    }
    
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T registerConfiguration(@NotNull Class<T> configClass) throws ConfigurationException {
        logger.info("Registering configuration: " + configClass.getName());
        
        // Check if already registered, return existing instance if so
        if (configurationInstances.containsKey(configClass)) {
            return (T) configurationInstances.get(configClass);
        }
        
        // Parse configuration metadata
        ConfigurationMetadata metadata = parser.parse(configClass);
        configurationMetadata.put(configClass, metadata);
        
        // Create configuration instance
        T instance = createConfigurationInstance(configClass);
        configurationInstances.put(configClass, instance);
        
        // Load configuration values
        loader.load(instance, metadata);
        
        // 如果需要，生成默认配置文件
        if (metadata.autoCreate) {
            loader.generateDefault(instance, metadata);
        }
        
        logger.info("Configuration registered: " + configClass.getSimpleName());
        return instance;
    }
    
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getConfiguration(@NotNull Class<T> configClass) {
        return (T) configurationInstances.get(configClass);
    }
    
    @Override
    @NotNull
    public <T> T getRequiredConfiguration(@NotNull Class<T> configClass) throws ConfigurationException {
        T config = getConfiguration(configClass);
        if (config == null) {
            throw new ConfigurationException(configClass, "Configuration not registered: " + configClass.getName());
        }
        return config;
    }
    
    @Override
    public boolean reloadConfiguration(@NotNull Class<?> configClass) {
        try {
            Object instance = configurationInstances.get(configClass);
            ConfigurationMetadata metadata = configurationMetadata.get(configClass);
            
            if (instance == null || metadata == null) {
                logger.warn("Configuration not found for reload: " + configClass.getName());
                return false;
            }
            
            // 创建旧配置的副本（用于变更通知）
            Object oldInstance = cloneConfiguration(instance);
            
            // 重新加载配置值
            loader.load(instance, metadata);
            
            // 通知监听器
            notifyConfigurationChanged(configClass, oldInstance, instance);
            
            totalReloads++;
            logger.info("Configuration reloaded: " + configClass.getSimpleName());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reload configuration: " + configClass.getName(), e);
            return false;
        }
    }
    
    @Override
    public int reloadAllConfigurations() {
        int successCount = 0;
        for (Class<?> configClass : configurationInstances.keySet()) {
            if (reloadConfiguration(configClass)) {
                successCount++;
            }
        }
        logger.info("Reloaded " + successCount + " configurations");
        return successCount;
    }
    
    @Override
    public boolean saveConfiguration(@NotNull Class<?> configClass) {
        try {
            Object instance = configurationInstances.get(configClass);
            ConfigurationMetadata metadata = configurationMetadata.get(configClass);
            
            if (instance == null || metadata == null) {
                return false;
            }
            
            loader.save(instance, metadata);
            logger.info("Configuration saved: " + configClass.getSimpleName());
            return true;
            
        } catch (IOException | IllegalAccessException e) {
            logger.error("Failed to save configuration: " + configClass.getName(), e);
            return false;
        }
    }
    
    @Override
    @NotNull
    public ConfigurationValidationResult validateConfiguration(@NotNull Class<?> configClass) {
        totalValidations++;
        
        Object instance = configurationInstances.get(configClass);
        ConfigurationMetadata metadata = configurationMetadata.get(configClass);
        
        if (instance == null || metadata == null) {
            return ConfigurationValidationResult.failure(Arrays.asList(
                new ValidationError("", "", "Configuration not registered", null)
            ));
        }
        
        return validator.validate(instance, metadata);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> void addConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener) {
        listeners.computeIfAbsent(configClass, k -> new CopyOnWriteArrayList<>())
                 .add((ConfigurationChangeListener<Object>) listener);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> void removeConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener) {
        List<ConfigurationChangeListener<Object>> classListeners = listeners.get(configClass);
        if (classListeners != null) {
            classListeners.remove((ConfigurationChangeListener<Object>) listener);
        }
    }
    
    @Override
    @NotNull
    public List<Class<?>> getRegisteredConfigurations() {
        return new ArrayList<>(configurationInstances.keySet());
    }
    
    @Override
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredConfigurations", configurationInstances.size());
        stats.put("totalReloads", totalReloads);
        stats.put("totalValidations", totalValidations);
        stats.put("listeners", listeners.values().stream().mapToInt(List::size).sum());
        return stats;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    
    /**
     * 创建配置实例
     */
    private <T> T createConfigurationInstance(Class<T> configClass) throws ConfigurationException {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(configClass, "Failed to create configuration instance", e);
        }
    }
    
    /**
     * 克隆配置实例
     */
    private Object cloneConfiguration(Object instance) {
        // 简化实现，实际项目中可能需要深度克隆
        try {
            return instance.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 通知配置变更监听器
     */
    private void notifyConfigurationChanged(Class<?> configClass, Object oldConfig, Object newConfig) {
        List<ConfigurationChangeListener<Object>> classListeners = listeners.get(configClass);
        if (classListeners != null) {
            for (ConfigurationChangeListener<Object> listener : classListeners) {
                try {
                    listener.onConfigurationChanged(oldConfig, newConfig);
                } catch (Exception e) {
                    logger.warn("Configuration change listener failed: " + e.getMessage());
                }
            }
        }
    }
}
