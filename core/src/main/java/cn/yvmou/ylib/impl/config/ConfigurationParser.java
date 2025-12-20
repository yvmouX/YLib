package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.config.AutoConfiguration;
import cn.yvmou.ylib.api.config.ConfigValue;
import cn.yvmou.ylib.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Configuration metadata parser.
 * Responsible for parsing annotations and building ConfigurationMetadata.
 */
public class ConfigurationParser {

    /**
     * Parses the metadata of a configuration class, including field annotations.
     *
     * @param configClass The configuration class to parse.
     * @return The parsed ConfigurationMetadata.
     * @throws ConfigurationException If an error occurs while parsing the configuration class.
     */
    public ConfigurationMetadata parse(@NotNull Class<?> configClass) throws ConfigurationException {
        final ConfigurationMetadata metadata = getConfigurationMetadata(configClass);

        // 解析字段注解
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

        // Retrieve the configuration name from the annotation; The autoConfig.value() default value is empty if not specified.
        // So if autoConfig.value() is empty, use the class name (without "Config" suffix) as the config name.
        // eg: PlayerTaskConfig.class -> "playertask"
        //     @AutoConfiguration("custom") -> "custom"
        String configName = autoConfig.value().isEmpty() ?
            configClass.getSimpleName().replace("Config", "").toLowerCase() :
            autoConfig.value();

        return new ConfigurationMetadata(
                configClass,
                configName,
                autoConfig.configFile(),
                autoConfig.autoCreate(),
                autoConfig.priority()
        );
    }
}
