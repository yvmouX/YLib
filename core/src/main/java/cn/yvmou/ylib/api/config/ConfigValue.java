package cn.yvmou.ylib.api.config;

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