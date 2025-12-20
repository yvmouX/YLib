package cn.yvmou.ylib.api.config;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置值注解
 * <p>
 * 标记在字段上，表示这个字段对应配置文件中的一个值。
 * YLib会自动从配置文件中读取值并注入到字段中。
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * @ConfigValue("database.host")
 * private String databaseHost = "localhost";
 * 
 * @ConfigValue(value = "server.port", required = true)
 * private int serverPort;
 * 
 * @ConfigValue(value = "features.enabled", description = "启用的功能列表")
 * private List<String> enabledFeatures = Arrays.asList("feature1", "feature2");
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {

    /**
     * 配置路径
     * <p>
     * 使用点号分隔的路径，如 "database.host" 对应配置文件中的：
     * </p>
     *
     * <pre>{@code
     * database:
     *   host: localhost
     * }</pre>
     *
     * @return 配置路径
     */
    @NotNull
    String value();
    
    /**
     * 配置描述
     * <p>
     * 用于生成配置文件时的注释说明。
     * </p>
     * 
     * @return 配置描述
     */
    @NotNull
    String description() default "";
    
    /**
     * 是否必需
     * <p>
     * 如果为true，当配置文件中缺少这个值时会抛出异常。
     * 如果为false，将使用字段的默认值。
     * </p>
     * 
     * @return 是否必需
     */
    boolean required() default false;
    
    /**
     * 验证表达式
     * <p>
     * 简单的验证规则，支持：
     * - "min:N" - 最小值（数字类型）
     * - "max:N" - 最大值（数字类型）
     * - "range:N-M" - 范围（数字类型）
     * - "length:N" - 长度（字符串类型）
     * - "regex:pattern" - 正则表达式（字符串类型）
     * - "enum:A,B,C" - 枚举值（字符串类型）
     * </p>
     * 
     * @return 验证表达式
     */
    @NotNull
    String validation() default "";
    
    /**
     * 是否敏感信息
     * <p>
     * 如果为true，在日志中会隐藏这个配置的值（显示为***）。
     * 适用于密码、API密钥等敏感信息。
     * </p>
     * 
     * @return 是否敏感信息
     */
    boolean sensitive() default false;
}