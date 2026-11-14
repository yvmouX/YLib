package cn.yvmou.ylib.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration value annotation.
 * <p>
 *     Mark a field to indicate that it should be populated with a value from the configuration file.
 *     YLib will automatically read the value from the file and inject it into the field.
 * </p>
 * <p>
 * 
 * <p>Usage example:</p>
 * <pre>
 *     {@code
 *     @ConfigValue("database.host")
 *     private String databaseHost = "localhost";
 *
 *     @ConfigValue(value = "server.port", required = true)
 *     private int serverPort;
 *
 *     @ConfigValue(value = "features.enabled", description = "Enabled features list")
 *     private List<String> enabledFeatures = Arrays.asList("feature1", "feature2");
 *     }
 * </pre>
 *
 * @author yvmou
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {

    /**
     * Configuration path.
     * <p>
     *     Use dot-separated path, e.g., "database.host" for:
     * </p>
     *
     * <pre>{@code
     * database:
     *   host: localhost
     * }</pre>
     *
     * @return Configuration path.
     */
    @NotNull
    String value();
    
    /**
     * Configuration description.
     * <p>
     *     Used to generate comments in the configuration file.
     * </p>
     * 
     * @return Configuration description.
     */
    @NotNull
    String description() default "";

    /**
     * Whether to refresh the comments in the file on every load.
     * <p>
     *     默认 {@code false}：文件里已有的注释不会被改动（只在键缺失、被自动补齐时写入 description）。
     *     写成 {@code true} 时，每次加载都会把该键上方的注释重写成 {@link #description()}——
     *     适合「说明是代码权威、需要跟着版本更新」的键。
     * </p>
     * <p>
     *     无论开关如何，注释里写了 {@code @keep} 的那几行都会被保留（整块跳过刷新），
     *     方便服主写「本服特有约定」这类代码里没有的说明。
     * </p>
     * <p>
     *     依赖 Bukkit 的注释 API（Spigot 1.18.1+）：更低版本上读写注释都会静默失效，本项不生效。
     * </p>
     *
     * @return Whether to refresh comments on load.
     */
    boolean refreshComment() default false;
    
    /**
     * Whether the value is required.
     * <p>
     *     If true, an exception will be thrown if the value is missing in the configuration file.
     *     If false, the field's default value will be used.
     * </p>
     * 
     * @return Whether the value is required.
     */
    boolean required() default false;
    
    /**
     * Validation expression.
     * <p>
     *     Simple validation rules, supported:
     * - "min:N" - Minimum value (numeric type)
     * - "max:N" - Maximum value (numeric type) 
     * - "range:N-M" - Range (numeric type)
     * - "length:N" - Length (string type)
     * - "regex:pattern" - Regular expression (string type)
     * - "enum:A,B,C" - Enum values (string type)
     * </p>
     * 
     * @return Validation expression.
     */
    @NotNull
    String validation() default "";
}