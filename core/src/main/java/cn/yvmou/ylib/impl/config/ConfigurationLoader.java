package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.exception.ConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Configuration loader.
 * Responsible for loading, saving, and generating configuration files.
 */
public class ConfigurationLoader {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ConfigurationLoader(@NotNull JavaPlugin plugin, @NotNull Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Loads configuration values from the file into the specified instance.
     *
     * @param instance Configuration instance to load values into
     * @param metadata Configuration metadata, containing field information
     * @throws ConfigurationException If an error occurs while loading configuration values
     */
    public void load(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws ConfigurationException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        
        // If file doesn't exist, skip loading (use default values from instance)
        // The file will be generated later by generateDefault
        if (!configFile.exists()) {
            logger.error("Configuration file {} does not exist. Skipping loading.", metadata.configFile);
            return;
        }

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
     * Saves configuration values from the specified instance into the file.
     *
     * @param instance Configuration instance to save values from
     * @param metadata Configuration metadata, containing field information
     * @throws IOException If an error occurs while saving configuration values
     * @throws IllegalAccessException If an error occurs while accessing field values
     */
    public void save(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws IOException, IllegalAccessException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Save configuration values to file
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            fieldMeta.field.setAccessible(true);
            Object value = fieldMeta.field.get(instance);
            config.set(fieldMeta.configPath, value);
        }
        
        config.save(configFile);
    }

    /**
     * Generates a default configuration file if it doesn't exist.
     * If the file already exists, this method does nothing.
     * <p>
     *     The generated file will contain all fields with their default values and comments.
     *     Comments will include field descriptions and whether they are required.
     * </p>
     *
     * @param instance Configuration instance to load default values from
     * @param metadata Configuration metadata, containing field information
     */
    public void generateDefault(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        
        if (configFile.exists()) {
            return;
        }
        
        try {
            boolean success = configFile.getParentFile().mkdirs();
            if (!success && !configFile.getParentFile().exists()) {
                logger.error("Failed to create parent directories for config file: " + metadata.configFile);
                return;
            }
            
            FileConfiguration config = new YamlConfiguration();
            
            // Set config version
            config.set("config-version", metadata.version);
            
            // Add configuration values and comments
            for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
                try {
                    fieldMeta.field.setAccessible(true);
                    Object defaultValue = fieldMeta.field.get(instance);
                    
                    config.set(fieldMeta.configPath, defaultValue);
                    
                    // Add comment (if description exists)
                    if (!fieldMeta.description.isEmpty()) {
                        // TODO: Find a way to support comments on older Spigot versions
                        // Try to set comments using reflection to support newer Spigot API (1.18+)
                        try {
                            java.lang.reflect.Method setCommentsMethod = config.getClass().getMethod("setComments", String.class, java.util.List.class);
                            setCommentsMethod.invoke(config, fieldMeta.configPath, java.util.Arrays.asList(fieldMeta.description.split("\n")));
                        } catch (Exception ignored) {
                             logger.debug("Comments not supported on this server version for field: " + fieldMeta.configPath);
                        }
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

    public void checkVersionAndMigrate(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        // If version is NONE, we don't migrate
        if (metadata.version.equals("NONE")) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        if (!configFile.exists()) {
            return; // File doesn't exist, generateDefault will handle it
        }

        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        String currentVersion = currentConfig.getString("config-version", "0.0.0");
        String targetVersion = metadata.version;

        // If versions don't match, we migrate
        if (!currentVersion.equals(targetVersion)) {
            logger.info("Migrating configuration " + metadata.configName + " from v" + currentVersion + " to v" + targetVersion);
            
            // Backup old config
            String backupName = metadata.configFile.replace(".yml", "") + "_backup_v" + currentVersion + ".yml";
            File backupFile = new File(plugin.getDataFolder(), backupName);
            
            if (configFile.renameTo(backupFile)) {
                logger.info("Backed up old configuration to " + backupName);
                
                // Generate new default file (with new structure and comments)
                generateDefault(instance, metadata);
                
                // Load both configs
                FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
                FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(backupFile);
                
                // Migrate values from old to new
                // We iterate over defined fields to ensure we only migrate valid data
                boolean modified = false;
                for (ConfigurationMetadata.FieldMetadata field : metadata.fields) {
                    if (oldConfig.contains(field.configPath)) {
                        newConfig.set(field.configPath, oldConfig.get(field.configPath));
                        modified = true;
                    }
                }
                
                // Ensure version is updated (generateDefault already set it, but just in case)
                newConfig.set("config-version", targetVersion);
                
                try {
                    newConfig.save(configFile);
                    logger.info("Configuration migration completed successfully.");
                } catch (IOException e) {
                    logger.error("Failed to save migrated configuration", e);
                }
                
            } else {
                logger.error("Failed to backup configuration file. Migration aborted to prevent data loss.");
            }
        }
    }

    /* Set field value, handling type conversion.
     * If the value is null, the field will be set to null.
     * If the value type matches the field type, it will be set directly.
     * Otherwise, the value will be converted to the field type.
     */
    private void setFieldValue(@NotNull Field field, @NotNull Object instance, @Nullable Object value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        
        if (value == null) {
            field.set(instance, null);
            return;
        }
        
        // Type matches, set value directly
        // eg: fieldType is Integer.class, value is "123"
        if (fieldType.isAssignableFrom(value.getClass())) {
            field.set(instance, value);
            return;
        }
        
        // Type conversion
        Object convertedValue = convertValue(value, fieldType);
        field.set(instance, convertedValue);
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(@NotNull Object value, @NotNull Class<?> targetType) {
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
        
        return value;
    }
}
