package cn.yvmou.ylib.config;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 配置元数据
 * <p>
 * 存储配置类的元数据信息，包括配置名称、文件名、字段信息等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public class ConfigurationMetadata {
    
    public final Class<?> configClass;
    public final String configName;
    public final String configFile;
    public final boolean autoCreate;
    public final String version;
    public final List<FieldMetadata> fields;
    
    /**
     * 构造函数
     * 
     * @param configClass 配置类
     * @param configName 配置名称
     * @param configFile 配置文件路径（相对插件数据目录，可含子目录；见 {@link #normalizeConfigFile(String)}）
     * @param autoCreate 是否自动创建
     * @param version 版本号
     */
    public ConfigurationMetadata(@NotNull Class<?> configClass, @NotNull String configName, 
                               @NotNull String configFile, boolean autoCreate, @NotNull String version) {
        this.configClass = configClass;
        this.configName = configName;
        this.configFile = normalizeConfigFile(configFile);
        this.autoCreate = autoCreate;
        this.version = version;
        this.fields = new ArrayList<>();
    }

    /**
     * 规范化配置路径：相对插件数据目录，可含子目录（父目录由加载器按需创建）。
     * <p>
     * 没写扩展名、或写的不是 {@code .yml} / {@code .yaml} 时补 {@code .yml}：
     * {@code "gui/gui"} 与 {@code "gui/gui.yml"} 都会落到 {@code <数据目录>/gui/gui.yml}；
     * 顺手把 {@code \\} 统一成 {@code /}，免得同一份配置在 Windows 与 Linux 上落到两个文件。
     * </p>
     *
     * @param configFile 注解里写的路径
     * @return 带扩展名、分隔符统一的路径
     */
    public static String normalizeConfigFile(String configFile) {
        String path = configFile == null ? "" : configFile.trim().replace('\\', '/');
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return path;
        }
        return path + ".yml";
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
        
        /**
         * 构造函数
         * 
         * @param field 字段
         * @param configPath 配置路径
         * @param description 描述
         * @param required 是否必需
         * @param validation 验证规则
         */
        public FieldMetadata(@NotNull Field field, @NotNull String configPath, @NotNull String description,
                           boolean required, @NotNull String validation) {
            this.field = field;
            this.configPath = configPath;
            this.description = description;
            this.required = required;
            this.validation = validation;
        }
    }
}