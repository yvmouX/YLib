package cn.yvmou.ylib.api.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AutoConfiguration
 * <p>
 *     YLib will automatically apply these configurations.
 * </p>
 * <p>
 *     usage:
 * </p>
 * <pre>
 *     {@code 
 *     @AutoConfiguration("database")
 *     public class DatabaseConfig {
 *         @ConfigValue("database.host")
 *         private String host = "localhost";
 *         
 *         @ConfigValue("database.port")
 *         private int port = 3306;
 *         
 *         // getters and setters...
 *     }
 *     }
 * </pre>
 * <p>
 *     In this example, YLib will automatically apply the configurations in DatabaseConfig class.
 * </p>
 * 
 * @author yvmou
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoConfiguration {
    
    /**
     * 配置名称，用于标识这个配置组
     * <p>
     * 如果不指定，将使用类名（去掉Config后缀）作为配置名称。
     * </p>
     * 
     * @return 配置名称
     */
    /**
     * Configuration name, used to identify this configuration group.
     * <p>
     * If not specified, the class name (without Config suffix) will be used as the configuration name.
     * </p>
     * 
     * @return Configuration name
     */
    @NotNull
    String value() default "";
    
    /**
     * Configuration file name.
     * 
     * @return Configuration file name
     */
    @NotNull
    String configFile();
    
    /**
     * Whether to automatically create default configuration file.
     * <p>
     * If true, when the configuration file does not exist, a default configuration file will be created with default values.
     * </p>
     * 
     * @return Whether to automatically create default configuration file
     */
    boolean autoCreate() default true;

    /**
     * Configuration version number.
     * <p>
     * Used for configuration file version control. When the version number in the code is higher than the version number in the configuration file,
     * an automatic backup and migration process will be triggered.
     * If not specified, it will be set to NONE.
     * When the version number is NONE, no automatic backup and migration process will be triggered.
     * </p>
     *
     * @return Configuration version number
     */
    @NotNull
    String version() default "NONE";
}