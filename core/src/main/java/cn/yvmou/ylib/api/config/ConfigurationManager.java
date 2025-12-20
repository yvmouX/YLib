package cn.yvmou.ylib.api.config;

import cn.yvmou.ylib.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Configuration manager interface.
 * <p>
 * Provides automatic configuration and convention-over-configuration features,
 * making configuration management simpler and more intelligent.
 * </p>
 * 
 * <p>Usage:</p>
 * <pre>
 *     {@code
 *     configManager.registerConfiguration(DatabaseConfig.class);
 * 
 *     // Get configuration instance
 *     DatabaseConfig dbConfig = configManager.getConfiguration(DatabaseConfig.class);
 *     // or
 *     DatabaseConfig dbConfig = configManager.registerConfiguration(DatabaseConfig.class);
 *     
 *     // Listen for configuration changes
 *     configManager.addConfigurationListener(DatabaseConfig.class, (oldConfig, newConfig) -> {
 *         // Handle configuration change
 *     });
 *     }
 * </pre>
 * 
 * @author yvmou
 * @since 1.0.0
 */
public interface ConfigurationManager {
    
    /**
     * Register configuration class.
     * <p>
     * Scans the configuration class for annotations, automatically loads configuration values,
     * and generates a default configuration file if it does not exist.
     * </p>
     * 
     * @param <T> The configuration type
     * @param configClass The configuration class
     * @return The configuration instance
     * @throws ConfigurationException If registration fails
     */
    @NotNull
    <T> T registerConfiguration(@NotNull Class<T> configClass) throws ConfigurationException;
    
    /**
     * Get configuration instance.
     * <p>
     * Returns the configuration instance for the specified configuration class.
     * If the configuration class is not registered, returns null.
     * </p>
     * 
     * @param <T> The configuration type
     * @param configClass The configuration class
     * @return The configuration instance, or null if not registered
     */
    @Nullable
    <T> T getConfiguration(@NotNull Class<T> configClass);
    
    /**
     * Get required configuration instance.
     * <p>
     * Returns the configuration instance for the specified configuration class.
     * If the configuration class is not registered, throws a ConfigurationException.
     * </p>
     * 
     * @param <T> The configuration type
     * @param configClass The configuration class
     * @return The configuration instance
     * @throws ConfigurationException If the configuration class is not registered
     */
    @NotNull
    <T> T getRequiredConfiguration(@NotNull Class<T> configClass) throws ConfigurationException;
    
    /**
     * Reload configuration.
     * <p>
     * Reloads the configuration values from the configuration file and updates the configuration instance.
     * </p>
     * 
     * @param configClass The configuration class
     * @return true if the reload is successful, false otherwise
     */
    boolean reloadConfiguration(@NotNull Class<?> configClass);
    
    /**
     * Reload all configurations.
     * <p>
     * Reloads the configuration values from the configuration file and updates all registered configuration instances.
     * </p>
     * 
     * @return The number of configurations successfully reloaded
     */
    int reloadAllConfigurations();
    
    /**
     * Save configuration to file.
     * <p>
     * Saves the current configuration instance values to the configuration file.
     * </p>
     * 
     * @param configClass The configuration class
     * @return true if the save is successful, false otherwise
     */
    boolean saveConfiguration(@NotNull Class<?> configClass);
    
    /**
     * Validate configuration.
     * <p>
     * Checks if the configuration values meet the validation rules defined in the configuration class.
     * </p>
     * 
     * @param configClass The configuration class
     * @return The validation result
     */
    @NotNull
    ConfigurationValidationResult validateConfiguration(@NotNull Class<?> configClass);
    
    /**
     * Add configuration change listener.
     * <p>
     * Registers a listener to be notified when the configuration values for the specified configuration class change.
     * </p>
     * 
     * @param <T> The configuration type
     * @param configClass The configuration class
     * @param listener The change listener
     */
    <T> void addConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener);
    
    /**
     * Remove configuration change listener.
     * <p>
     * Removes the specified listener from the list of change listeners for the specified configuration class.
     * </p>
     * 
     * @param <T> The configuration type
     * @param configClass The configuration class
     * @param listener The change listener
     */
    <T> void removeConfigurationListener(@NotNull Class<T> configClass, @NotNull ConfigurationChangeListener<T> listener);
    
    /**
     * Get all registered configuration classes.
     * <p>
     * Returns a list of all configuration classes that have been registered with the ConfigurationManager.
     * </p>
     * 
     * @return The list of registered configuration classes
     */
    @NotNull
    List<Class<?>> getRegisteredConfigurations();
    
    /**
     * Get configuration statistics.
     * <p>
     * Returns a map containing statistics about the registered configurations, such as the number of configurations,
     * the number of listeners, and the total number of configuration values.
     * </p>
     * 
     * @return The map of configuration statistics
     */
    @NotNull
    Map<String, Object> getStatistics();
    
    /**
     * Configuration change listener.
     * <p>
     * Listener interface for receiving notifications when the configuration values for a specific configuration class change.
     * </p>
     * 
     * @param <T> The configuration type
     */
    @FunctionalInterface
    interface ConfigurationChangeListener<T> {
        /**
         * Called when the configuration values change.
         * <p>
         * This method is invoked when the configuration values for the specified configuration class change.
         * </p>
         * 
         * @param oldConfiguration The old configuration instance, or null if not available
         * @param newConfiguration The new configuration instance
         */
        void onConfigurationChanged(@Nullable T oldConfiguration, @NotNull T newConfiguration);
    }
}