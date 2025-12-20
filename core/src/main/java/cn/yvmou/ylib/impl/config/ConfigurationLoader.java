package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.exception.ConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Configuration loader.
 * Responsible for loading, saving, and generating configuration files.
 */
public class ConfigurationLoader {

    private final JavaPlugin plugin;
    private final LoggerService logger;

    public ConfigurationLoader(@NotNull JavaPlugin plugin, @NotNull LoggerService logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Loads configuration values from the file into the specified instance.
     *
     * @param instance 配置值将被设置到的实例
     * @param metadata 配置元数据，包含字段信息
     * @throws ConfigurationException 如果加载配置时发生错误
     */
    public void load(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws ConfigurationException {
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
     * 保存配置
     */
    public void save(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws IOException, IllegalAccessException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // 保存配置值
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            fieldMeta.field.setAccessible(true);
            Object value = fieldMeta.field.get(instance);
            config.set(fieldMeta.configPath, value);
        }
        
        config.save(configFile);
    }

    /**
     * 生成默认配置文件
     */
    public void generateDefault(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
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
    @SuppressWarnings({"unchecked", "rawtypes"})
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
}
