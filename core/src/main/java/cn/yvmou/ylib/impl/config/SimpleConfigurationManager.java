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
    
    // Configuration instance cache 
    // Map Class<?> -> ClassInstance
    private final Map<Class<?>, Object> configurationInstances = new ConcurrentHashMap<>();
    
    // Configuration metadata cache 
    // Map Class<?> -> ConfigurationMetadata
    private final Map<Class<?>, ConfigurationMetadata> configurationMetadata = new ConcurrentHashMap<>();
    
    // Configuration change listeners 
    // Map Class<?> -> List<ConfigurationChangeListener<Object>>
    private final Map<Class<?>, List<ConfigurationChangeListener<Object>>> listeners = new ConcurrentHashMap<>();
    
    // Statistics 
    // Total reloads count
    private volatile int totalReloads = 0;
    // Total validations count
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
        configurationMetadata.put(configClass, metadata); // configurationMetadata already cached
        
        // Create configuration instance
        T instance = createConfigurationInstance(configClass); // configurationInstances already cached
        configurationInstances.put(configClass, instance);
        
        // Check version and migrate if needed
        loader.checkVersionAndMigrate(instance, metadata);

        // Load configuration values
        // if the config file doesn't exist, this method does nothing
        loader.load(instance, metadata);
        
        // Generate default config file if autoCreate is enabled
        // And if the config already exists, this method does nothing
        if (metadata.autoCreate) {
            loader.generateDefault(instance, metadata);
        }
        
        // Validate configuration
        ConfigurationValidationResult validationResult = validator.validate(instance, metadata);
        if (!validationResult.isValid()) {
            for (ValidationError error : validationResult.getErrors()) {
                logger.error(error.toString());
            }
            throw new ConfigurationException(configClass, "Configuration validation failed: " + validationResult.getErrorCount() + " errors found.");
        }
        if (validationResult.hasWarnings()) {
            for (ConfigurationValidationResult.ValidationWarning warning : validationResult.getWarnings()) {
                logger.warn(warning.toString());
            }
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
            
            // Create a copy of the old configuration instance (for change notification)
            Object oldInstance = cloneConfiguration(instance, metadata);
            
            // Reload configuration values
            loader.load(instance, metadata);
            
            // Validate configuration
            ConfigurationValidationResult validationResult = validator.validate(instance, metadata);
            if (!validationResult.isValid()) {
                logger.error("Configuration validation failed during reload for: " + configClass.getSimpleName());
                for (ValidationError error : validationResult.getErrors()) {
                    logger.error(error.toString());
                }
                
                // Rollback to old instance
                if (oldInstance != null) {
                    logger.warn("Rolling back to previous valid configuration...");
                    copyConfiguration(oldInstance, instance, metadata);
                }
                
                return false;
            }
            if (validationResult.hasWarnings()) {
                for (ConfigurationValidationResult.ValidationWarning warning : validationResult.getWarnings()) {
                    logger.warn(warning.toString());
                }
            }
            
            // Notify listeners
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
    private <T> T createConfigurationInstance(Class<T> configClass) throws ConfigurationException {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(configClass, "Failed to create configuration instance", e);
        }
    }
    
    private Object cloneConfiguration(Object instance, ConfigurationMetadata metadata) {
        try {
            Object clone = instance.getClass().getDeclaredConstructor().newInstance();
            copyConfiguration(instance, clone, metadata);
            return clone;
        } catch (Exception e) {
            logger.error("Failed to clone configuration instance: " + e.getMessage(), e);
            return null;
        }
    }

    private void copyConfiguration(Object source, Object target, ConfigurationMetadata metadata) {
        // Copy field values
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            try {
                Object value = fieldMeta.field.get(source);
                fieldMeta.field.set(target, value);
            } catch (IllegalAccessException e) {
                // Should not happen as we set accessible in parser
                logger.error("Failed to copy field: " + fieldMeta.configPath, e);
            }
        }
    }
    
    private void notifyConfigurationChanged(Class<?> configClass, Object oldConfig, Object newConfig) {
        List<ConfigurationChangeListener<Object>> classListeners = listeners.get(configClass);
        if (classListeners != null) {
            for (ConfigurationChangeListener<Object> listener : classListeners) {
                try {
                    listener.onConfigurationChanged(oldConfig, newConfig);
                } catch (Exception e) {
                    logger.error("Configuration change listener failed: " + e.getMessage(), e);
                }
            }
        }
    }
}
