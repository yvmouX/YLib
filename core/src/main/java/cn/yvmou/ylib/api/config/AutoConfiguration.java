package cn.yvmou.ylib.api.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动配置注解
 * <p>
 * 标记在类上，表示这个类包含自动配置信息。
 * YLib会自动扫描并应用这些配置。
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * @AutoConfiguration
 * public class DatabaseConfig {
 *     @ConfigValue("database.host")
 *     private String host = "localhost";
 *     
 *     @ConfigValue("database.port")
 *     private int port = 3306;
 *     
 *     // getters and setters...
 * }
 * }</pre>
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
    @NotNull
    String value() default "";
    
    /**
     * 配置文件名称
     * <p>
     * 默认使用config.yml，可以指定其他配置文件。
     * </p>
     * 
     * @return 配置文件名称
     */
    @NotNull
    String configFile() default "config.yml";
    
    /**
     * 是否自动创建默认配置
     * <p>
     * 如果为true，当配置文件不存在时，会自动创建包含默认值的配置文件。
     * </p>
     * 
     * @return 是否自动创建默认配置
     */
    boolean autoCreate() default true;
    
    /**
     * 配置优先级
     * <p>
     * 数值越小优先级越高，用于控制多个配置的加载顺序。
     * </p>
     * 
     * @return 优先级
     */
    int priority() default 0;
}