package cn.yvmou.ylib.common.config;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置元数据
 * <p>
 * 存储配置类的元数据信息，包括配置名称、文件名、字段信息等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class ConfigurationMetadata {
    
    public final Class<?> configClass;
    public final String configName;
    public final String configFile;
    public final boolean autoCreate;
    public final int priority;
    public final List<FieldMetadata> fields;
    
    /**
     * 构造函数
     * 
     * @param configClass 配置类
     * @param configName 配置名称
     * @param configFile 配置文件名
     * @param autoCreate 是否自动创建
     * @param priority 优先级
     */
    public ConfigurationMetadata(@NotNull Class<?> configClass, @NotNull String configName, 
                               @NotNull String configFile, boolean autoCreate, int priority) {
        this.configClass = configClass;
        this.configName = configName;
        this.configFile = configFile;
        this.autoCreate = autoCreate;
        this.priority = priority;
        this.fields = new ArrayList<>();
    }
    
    /**
     * 添加字段元数据
     * 
     * @param fieldMetadata 字段元数据
     */
    public void addField(@NotNull FieldMetadata fieldMetadata) {
        fields.add(fieldMetadata);
    }
    
    /**
     * 字段元数据
     */
    public static class FieldMetadata {
        public final Field field;
        public final String configPath;
        public final String description;
        public final boolean required;
        public final String validation;
        public final boolean sensitive;
        
        /**
         * 构造函数
         * 
         * @param field 字段
         * @param configPath 配置路径
         * @param description 描述
         * @param required 是否必需
         * @param validation 验证规则
         * @param sensitive 是否敏感
         */
        public FieldMetadata(@NotNull Field field, @NotNull String configPath, @NotNull String description,
                           boolean required, @NotNull String validation, boolean sensitive) {
            this.field = field;
            this.configPath = configPath;
            this.description = description;
            this.required = required;
            this.validation = validation;
            this.sensitive = sensitive;
        }
    }
}