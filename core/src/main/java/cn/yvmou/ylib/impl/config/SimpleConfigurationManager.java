package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.config.*;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult.ValidationError;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult.ValidationWarning;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.exception.ConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * 简单配置管理器实现
 * <p>
 * 提供基于注解的自动配置功能，支持配置验证、热重载和变更监听。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public class SimpleConfigurationManager implements ConfigurationManager {
    
    private final JavaPlugin plugin;
    private final LoggerService logger;
    
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
        this.plugin = plugin;
        this.logger = logger;
    }
    
    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T registerConfiguration(@NotNull Class<T> configClass) throws ConfigurationException {
        logger.info("Registering configuration: " + configClass.getName());
        
        // 检查是否已注册, 如果已经注册，返回其实例
        if (configurationInstances.containsKey(configClass)) {
            return (T) configurationInstances.get(configClass);
        }
        
        // 解析配置元数据
        ConfigurationMetadata metadata = parseConfigurationMetadata(configClass);
        configurationMetadata.put(configClass, metadata);
        
        // 创建配置实例
        T instance = createConfigurationInstance(configClass);
        configurationInstances.put(configClass, instance);
        
        // 加载配置值
        loadConfigurationValues(instance, metadata);
        
        // 如果需要，生成默认配置文件
        if (metadata.autoCreate) {
            generateDefaultConfigFile(metadata);
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
            loadConfigurationValues(instance, metadata);
            
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
            
            File configFile = new File(plugin.getDataFolder(), metadata.configFile);
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // 保存配置值
            for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
                try {
                    fieldMeta.field.setAccessible(true);
                    Object value = fieldMeta.field.get(instance);
                    config.set(fieldMeta.configPath, value);
                } catch (IllegalAccessException e) {
                    logger.warn("Failed to save field: " + fieldMeta.field.getName() + " - " + e.getMessage());
                }
            }
            
            config.save(configFile);
            logger.info("Configuration saved: " + configClass.getSimpleName());
            return true;
            
        } catch (IOException e) {
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
        
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            try {
                fieldMeta.field.setAccessible(true);
                Object value = fieldMeta.field.get(instance);
                
                // 检查必需字段
                if (fieldMeta.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
                    errors.add(new ValidationError(
                        fieldMeta.field.getName(),
                        fieldMeta.configPath,
                        "Required field is missing or empty",
                        value
                    ));
                    continue;
                }
                
                // 验证字段值
                if (value != null && !fieldMeta.validation.isEmpty()) {
                    ValidationResult result = validateFieldValue(value, fieldMeta.validation);
                    if (!result.valid) {
                        errors.add(new ValidationError(
                            fieldMeta.field.getName(),
                            fieldMeta.configPath,
                            result.message,
                            value
                        ));
                    }
                }
                
            } catch (IllegalAccessException e) {
                errors.add(new ValidationError(
                    fieldMeta.field.getName(),
                    fieldMeta.configPath,
                    "Failed to access field: " + e.getMessage(),
                    null
                ));
            }
        }
        
        if (errors.isEmpty()) {
            return warnings.isEmpty() ? 
                ConfigurationValidationResult.success() : 
                ConfigurationValidationResult.withWarnings(warnings);
        } else {
            return ConfigurationValidationResult.failure(errors);
        }
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
     * 解析配置类的元数据，获取字段元数据
     */
    private ConfigurationMetadata parseConfigurationMetadata(Class<?> configClass) throws ConfigurationException {
        final ConfigurationMetadata metadata = getConfigurationMetadata(configClass);

        // 解析字段
        Field[] fields = configClass.getDeclaredFields();
        for (Field field : fields) {
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            if (configValue != null) {
                metadata.addField(new ConfigurationMetadata.FieldMetadata(
                    field,
                    configValue.value(),
                    configValue.description(),
                    configValue.required(),
                    configValue.validation(),
                    configValue.sensitive()
                ));
            }
        }
        
        return metadata;
    }

    private @NotNull ConfigurationMetadata getConfigurationMetadata(Class<?> configClass) {
        AutoConfiguration autoConfig = configClass.getAnnotation(AutoConfiguration.class);
        if (autoConfig == null) {
            throw new ConfigurationException(configClass, "Class must be annotated with @AutoConfiguration");
        }

        // 获取用于标识这个配置组的配置名称，如果autoConfig.value()为空，则使用使用类名（去掉Config后缀）作为配置名称。
        String configName = autoConfig.value().isEmpty() ?
            configClass.getSimpleName().replace("Config", "").toLowerCase() :
            autoConfig.value();

        //configClass – 配置类
        //configName – 配置名称
        //configFile – 配置文件名
        //autoCreate – 是否自动创建
        //priority – 优先
        return new ConfigurationMetadata(
                configClass,
                configName,
                autoConfig.configFile(),
                autoConfig.autoCreate(),
                autoConfig.priority()
        );
    }

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
     * 加载配置值到实例
     */
    private void loadConfigurationValues(Object instance, ConfigurationMetadata metadata) throws ConfigurationException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            try {
                fieldMeta.field.setAccessible(true);
                
                if (config.contains(fieldMeta.configPath)) {
                    Object value = config.get(fieldMeta.configPath);
                    setFieldValue(fieldMeta.field, instance, value);
                } else if (fieldMeta.required) {
                    throw new ConfigurationException(
                        metadata.configClass,
                        fieldMeta.configPath,
                        "Required configuration value is missing"
                    );
                }
                
            } catch (IllegalAccessException e) {
                throw new ConfigurationException(
                    metadata.configClass,
                    fieldMeta.configPath,
                    "Failed to set field value",
                    e
                );
            }
        }
    }
    
    /**
     * 设置字段值，处理类型转换
     */
    private void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        
        if (value == null) {
            field.set(instance, null);
            return;
        }
        
        // 类型匹配，直接设置
        if (fieldType.isAssignableFrom(value.getClass())) {
            field.set(instance, value);
            return;
        }
        
        // 类型转换
        Object convertedValue = convertValue(value, fieldType);
        field.set(instance, convertedValue);
    }
    
    /**
     * 值类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        
        String stringValue = value.toString();
        
        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(stringValue);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(stringValue);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(stringValue);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(stringValue);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(stringValue);
        } else if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, stringValue.toUpperCase());
        }
        
        // 默认返回原值
        return value;
    }
    
    /**
     * 生成默认配置文件
     */
    private void generateDefaultConfigFile(ConfigurationMetadata metadata) {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        
        if (configFile.exists()) {
            return; // 文件已存在，不覆盖
        }
        
        try {
            configFile.getParentFile().mkdirs();
            FileConfiguration config = new YamlConfiguration();
            
            // 添加配置值和注释
            for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
                try {
                    fieldMeta.field.setAccessible(true);
                    Object instance = configurationInstances.get(metadata.configClass);
                    Object defaultValue = fieldMeta.field.get(instance);
                    
                    config.set(fieldMeta.configPath, defaultValue);
                    
                    // 添加注释（如果有描述）
                    if (!fieldMeta.description.isEmpty()) {
                        // YAML注释需要特殊处理，这里简化处理
                        logger.info("Field " + fieldMeta.configPath + ": " + fieldMeta.description);
                    }
                    
                } catch (IllegalAccessException e) {
                    logger.warn("Failed to get default value for field: " + fieldMeta.field.getName());
                }
            }
            
            config.save(configFile);
            logger.info("Generated default configuration file: " + metadata.configFile);
            
        } catch (IOException e) {
            logger.error("Failed to generate default configuration file: " + metadata.configFile, e);
        }
    }
    
    /**
     * 验证字段值
     */
    private ValidationResult validateFieldValue(Object value, String validation) {
        if (validation.isEmpty()) {
            return new ValidationResult(true, "");
        }
        
        String[] rules = validation.split(",");
        for (String rule : rules) {
            rule = rule.trim();
            
            if (rule.startsWith("min:")) {
                double min = Double.parseDouble(rule.substring(4));
                if (value instanceof Number && ((Number) value).doubleValue() < min) {
                    return new ValidationResult(false, "Value must be at least " + min);
                }
            } else if (rule.startsWith("max:")) {
                double max = Double.parseDouble(rule.substring(4));
                if (value instanceof Number && ((Number) value).doubleValue() > max) {
                    return new ValidationResult(false, "Value must be at most " + max);
                }
            } else if (rule.startsWith("range:")) {
                String range = rule.substring(6);
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0]);
                    double max = Double.parseDouble(parts[1]);
                    if (value instanceof Number) {
                        double val = ((Number) value).doubleValue();
                        if (val < min || val > max) {
                            return new ValidationResult(false, "Value must be between " + min + " and " + max);
                        }
                    }
                }
            } else if (rule.startsWith("length:")) {
                int length = Integer.parseInt(rule.substring(7));
                if (value instanceof String && ((String) value).length() != length) {
                    return new ValidationResult(false, "String length must be " + length);
                }
            } else if (rule.startsWith("regex:")) {
                String pattern = rule.substring(6);
                if (value instanceof String && !Pattern.matches(pattern, (String) value)) {
                    return new ValidationResult(false, "Value does not match pattern: " + pattern);
                }
            } else if (rule.startsWith("enum:")) {
                String enumValues = rule.substring(5);
                String[] allowedValues = enumValues.split("\\|");
                boolean found = false;
                for (String allowedValue : allowedValues) {
                    if (allowedValue.trim().equals(value.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return new ValidationResult(false, "Value must be one of: " + enumValues);
                }
            }
        }
        
        return new ValidationResult(true, "");
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
    @SuppressWarnings("unchecked")
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
    
    // ========== 内部类 ==========
    
    /**
     * 验证结果
     */
    private static class ValidationResult {
        final boolean valid;
        final String message;
        
        ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}