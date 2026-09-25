package cn.yvmou.ylib.config;

import cn.yvmou.ylib.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration loader.
 * Responsible for loading, saving, and generating configuration files.
 * <p>
 * 支持的类型绑定：
 * <ul>
 *     <li>标量：String / 基本类型及包装类 / 枚举（大小写不敏感）</li>
 *     <li>{@code List<T>}：元素按目标泛型转换</li>
 *     <li>数组（{@code String[]}、{@code int[]}…）：文件里照旧写成 YAML 列表，按组件类型转换后装成数组；
 *         写默认值时也序列化成列表，不会写出 {@code [Ljava.lang.String;@…}</li>
 *     <li>{@code Map<String, V>}（动态键名 section）：V 可为标量、List 或配置 POJO
 *         （POJO 内字段用 {@code @ConfigValue} 声明相对路径，支持再嵌套 Map/POJO）</li>
 * </ul>
 */
public class ConfigurationLoader {
    private final Plugin plugin;
    private final Logger logger;

    public ConfigurationLoader(@NotNull Plugin plugin, @NotNull Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /**
     * Loads configuration values from the file into the specified instance.
     *
     * @param instance Configuration instance to load values into
     * @param metadata Configuration metadata, containing field information
     * @throws ConfigurationException If an error occurs while loading configuration values
     */
    public void load(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws ConfigurationException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);

        // If file doesn't exist, skip loading (use default values from instance)
        // The file will be generated later by generateDefault
        if (!configFile.exists()) {
            logger.error("Configuration file {} does not exist. Skipping loading.", metadata.configFile);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            try {
                fieldMeta.field.setAccessible(true);

                if (config.contains(fieldMeta.configPath)) {
                    Object value = config.get(fieldMeta.configPath);
                    setFieldValue(fieldMeta.field, instance, value);
                } else if (fieldMeta.required) {
                    throw new ConfigurationException(
                        metadata.configClass,
                        fieldMeta.configPath,
                        "Required configuration value is missing"
                    );
                }

            } catch (IllegalAccessException e) {
                throw new ConfigurationException(
                    metadata.configClass,
                    fieldMeta.configPath,
                    "Failed to set field value",
                    e
                );
            }
        }
    }

    /**
     * Saves configuration values from the specified instance into the file.
     * Map / POJO 字段会先序列化为可写回 YAML 的结构，用户文件中未声明的键与已有注释会保留。
     *
     * @param instance Configuration instance to save values from
     * @param metadata Configuration metadata, containing field information
     * @throws IOException If an error occurs while saving configuration values
     * @throws IllegalAccessException If an error occurs while accessing field values
     */
    public void save(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) throws IOException, IllegalAccessException {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Save configuration values to file
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            fieldMeta.field.setAccessible(true);
            Object value = fieldMeta.field.get(instance);
            config.set(fieldMeta.configPath, serializeFieldValue(value));
        }

        config.save(configFile);
    }

    /**
     * Generates a default configuration file if it doesn't exist.
     * If the file already exists, this method does nothing.
     * <p>
     *     The generated file will contain all fields with their default values and comments.
     *     Comments will include field descriptions and whether they are required.
     *     Map 字段若带有默认实例（如固定的按钮、食物定义），其内容会序列化写入文件。
     * </p>
     *
     * @param instance Configuration instance to load default values from
     * @param metadata Configuration metadata, containing field information
     */
    public void generateDefault(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);

        if (configFile.exists()) {
            return;
        }

        try {
            boolean success = configFile.getParentFile().mkdirs();
            if (!success && !configFile.getParentFile().exists()) {
                logger.error("Failed to create parent directories for config file: " + metadata.configFile);
                return;
            }

            FileConfiguration config = new YamlConfiguration();

            // Set config version
            config.set("config-version", metadata.version);

            // Add configuration values and comments
            for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
                try {
                    fieldMeta.field.setAccessible(true);
                    Object defaultValue = fieldMeta.field.get(instance);

                    config.set(fieldMeta.configPath, serializeFieldValue(defaultValue));

                    // Add comment (if description exists)
                    setComments(config, fieldMeta.configPath, fieldMeta.description);

                } catch (IllegalAccessException e) {
                    logger.warn("Failed to get default value for field: " + fieldMeta.field.getName());
                }
            }

            config.save(configFile);
            logger.info("Generated default configuration file: " + metadata.configFile);

        } catch (IOException e) {
            logger.error("Failed to generate default configuration file: " + metadata.configFile, e);
        }
    }

    /**
     * 为文件中缺失的声明键补齐默认值（不覆盖用户已有值与注释），并刷新标了
     * {@code refreshComment = true} 的键的注释。
     * <ul>
     *     <li>标量/List 字段：整键缺失时补默认值 + description 注释</li>
     *     <li>Map 字段：按默认实例的 key 集逐 key 补齐（用户已删除/修改的已有 key 不受影响）</li>
     *     <li>required 字段不自动补齐，缺失仍由 {@link #load} 报错</li>
     *     <li>{@code refreshComment} 字段：注释与 description 不一致时重写（写了 {@code @keep} 的整块跳过）</li>
     * </ul>
     * 典型场景：插件升级后新增配置键，老用户的文件自动获得新键。
     *
     * @param instance Configuration instance（此时字段值应为 Java 默认值）
     * @param metadata Configuration metadata, containing field information
     */
    public void mergeMissingKeys(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        if (!configFile.exists()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            boolean modified = false;

            for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
                fieldMeta.field.setAccessible(true);

                if (!config.contains(fieldMeta.configPath)) {
                    if (fieldMeta.required) {
                        continue; // required 缺失由 load 抛错提示，不静默补齐
                    }
                    Object defaultValue = fieldMeta.field.get(instance);
                    if (defaultValue == null) {
                        continue;
                    }
                    config.set(fieldMeta.configPath, serializeFieldValue(defaultValue));
                    setComments(config, fieldMeta.configPath, fieldMeta.description);
                    modified = true;
                    logger.info("Added missing configuration key \"{}\" to {}", fieldMeta.configPath, metadata.configFile);
                    continue;
                }

                // 已存在：注释按 refreshComment 刷新，Map 字段还要按默认实例逐 key 深度补齐
                modified |= refreshCommentIfNeeded(config, fieldMeta);
                if (Map.class.isAssignableFrom(fieldMeta.field.getType())) {
                    modified |= mergeMapEntries(config, fieldMeta, instance);
                }
            }

            // 只有真的改了才写盘：注释刷新也计入 modified，否则改完 description 重启也看不到效果；
            // 反之（内容一致时）不写，免得每次启动 mtime 都变、用户以为配置被动了
            if (modified) {
                config.save(configFile);
                logger.info("Merged missing keys into configuration file: " + metadata.configFile);
            }
        } catch (Exception e) {
            logger.error("Failed to merge missing configuration keys for " + metadata.configFile, e);
        }
    }

    /**
     * 将默认 Map 实例中用户文件缺失的 key 补进对应 section。
     */
    private boolean mergeMapEntries(@NotNull FileConfiguration config, @NotNull ConfigurationMetadata.FieldMetadata fieldMeta,
                                    @NotNull Object instance) throws IllegalAccessException {
        Object defaultMapObj = fieldMeta.field.get(instance);
        if (!(defaultMapObj instanceof Map)) {
            return false;
        }

        ConfigurationSection section = config.getConfigurationSection(fieldMeta.configPath);
        if (section == null) {
            return false; // 整节缺失的场景由上层整键补齐处理
        }

        boolean modified = false;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) defaultMapObj).entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (section.contains(key)) {
                continue;
            }
            Object serialized = serializeFieldValue(entry.getValue());
            if (serialized == null) {
                continue;
            }
            config.set(fieldMeta.configPath + "." + key, serialized);
            modified = true;
        }
        return modified;
    }

    public void checkVersionAndMigrate(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        // If version is NONE, we don't migrate
        if (metadata.version.equals("NONE")) {
            return;
        }

        File configFile = new File(plugin.getDataFolder(), metadata.configFile);
        if (!configFile.exists()) {
            return; // File doesn't exist, generateDefault will handle it
        }

        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        String currentVersion = currentConfig.getString("config-version", "0.0.0");
        String targetVersion = metadata.version;

        // If versions don't match, we migrate
        if (!currentVersion.equals(targetVersion)) {
            logger.info("Migrating configuration " + metadata.configName + " from v" + currentVersion + " to v" + targetVersion);

            // Backup old config
            String backupName = metadata.configFile.replace(".yml", "") + "_backup_v" + currentVersion + ".yml";
            File backupFile = new File(plugin.getDataFolder(), backupName);

            if (configFile.renameTo(backupFile)) {
                logger.info("Backed up old configuration to " + backupName);

                // Generate new default file (with new structure and comments)
                generateDefault(instance, metadata);

                // Load both configs
                FileConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);
                FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(backupFile);

                // Migrate values from old to new
                // We iterate over defined fields to ensure we only migrate valid data
                boolean modified = false;
                for (ConfigurationMetadata.FieldMetadata field : metadata.fields) {
                    if (oldConfig.contains(field.configPath)) {
                        Object oldValue = oldConfig.get(field.configPath);
                        // MemorySection 转为普通 Map 写回，避免跨 Configuration 复制 section 的兼容性问题
                        if (oldValue instanceof ConfigurationSection) {
                            oldValue = sectionToMap((ConfigurationSection) oldValue);
                        }
                        newConfig.set(field.configPath, oldValue);
                        modified = true;
                    }
                }

                // Ensure version is updated (generateDefault already set it, but just in case)
                newConfig.set("config-version", targetVersion);

                try {
                    newConfig.save(configFile);
                    logger.info("Configuration migration completed successfully.");
                } catch (IOException e) {
                    logger.error("Failed to save migrated configuration", e);
                }

            } else {
                logger.error("Failed to backup configuration file. Migration aborted to prevent data loss.");
            }
        }
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  字段赋值 | Field Binding
       └─────────────────────────────────────────────────────────────────┘
     */

    /* Set field value, handling type conversion.
     * If the value is null, the field will be set to null.
     * If the value type matches the field type, it will be set directly.
     * Otherwise, the value will be converted to the field type.
     */
    private void setFieldValue(@NotNull Field field, @NotNull Object instance, @Nullable Object value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();

        if (value == null) {
            field.set(instance, null);
            return;
        }

        // Map 字段（动态键名 section）
        if (Map.class.isAssignableFrom(fieldType)) {
            if (value instanceof ConfigurationSection) {
                field.set(instance, deserializeMap((ConfigurationSection) value, field));
            } else if (value instanceof Map) {
                field.set(instance, value);
            }
            // 标量配到 Map 字段属于结构错误，保留 Java 默认值
            return;
        }

        setPlainValue(field, instance, value);
    }

    /**
     * 非 Map 字段的赋值：数组 / List 元素转换、直接赋值、标量转换。
     */
    private void setPlainValue(@NotNull Field field, @NotNull Object instance, @Nullable Object value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();

        if (value == null) {
            field.set(instance, null);
            return;
        }

        // 数组：YAML 只能给出 List，按组件类型逐个转换后装成数组（String[] 这类字段因此可以直接声明）
        if (fieldType.isArray() && value instanceof List) {
            field.set(instance, convertArray((List<?>) value, fieldType.getComponentType()));
            return;
        }

        // List：元素按目标泛型逐个转换，避免类型擦除后读取时才报 ClassCastException
        if (List.class.isAssignableFrom(fieldType) && value instanceof List) {
            field.set(instance, convertList((List<?>) value, field));
            return;
        }

        // Type matches, set value directly
        if (fieldType.isAssignableFrom(value.getClass())) {
            field.set(instance, value);
            return;
        }

        // Type conversion
        Object convertedValue = convertValue(value, fieldType);
        field.set(instance, convertedValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(@NotNull Object value, @NotNull Class<?> targetType) {
        if (value == null) return null;

        String stringValue = value.toString();

        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(stringValue);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(stringValue);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(stringValue);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(stringValue);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(stringValue);
        } else if (targetType.isEnum()) {
            // 先精确匹配，再大小写不敏感匹配（枚举常量不要求全大写）
            for (Object constant : targetType.getEnumConstants()) {
                if (((Enum) constant).name().equalsIgnoreCase(stringValue)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("No enum constant " + targetType.getName() + " for value \"" + stringValue + "\"");
        }

        return value;
    }

    @NotNull
    private List<Object> convertList(@NotNull List<?> raw, @NotNull Field field) {
        Class<?> elementType = String.class;
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                elementType = (Class<?>) args[0];
            }
        }

        List<Object> result = new ArrayList<>(raw.size());
        for (Object element : raw) {
            result.add(elementType == String.class ? String.valueOf(element) : convertValue(element, elementType));
        }
        return result;
    }

    /**
     * List → 数组：组件类型是确定的（不像 List 要靠泛型擦除后的签名去猜），因此逐个转换即可。
     * 元素转换规则与 {@link #convertList} 一致；组件类型为基本类型时由 {@link Array} 负责装箱。
     */
    @NotNull
    private Object convertArray(@NotNull List<?> raw, @NotNull Class<?> componentType) {
        Object array = Array.newInstance(componentType, raw.size());
        for (int index = 0; index < raw.size(); index++) {
            Object element = raw.get(index);
            Array.set(array, index, componentType == String.class
                    ? String.valueOf(element) : convertValue(element, componentType));
        }
        return array;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  Map / POJO 绑定 | Dynamic Section Binding
       └─────────────────────────────────────────────────────────────────┘
     */

    /**
     * 将动态 section 反序列化为 Map。值类型取自字段泛型（标量 / List / 配置 POJO）。
     */
    @NotNull
    private Map<String, Object> deserializeMap(@NotNull ConfigurationSection section, @NotNull Field field) throws IllegalAccessException {
        Type valueType = resolveMapValueType(field);

        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            result.put(key, deserializeMapValue(section.get(key), valueType));
        }
        return result;
    }

    @Nullable
    private Type resolveMapValueType(@NotNull Field field) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType)) {
            return null;
        }
        Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
        return args.length == 2 ? args[1] : null;
    }

    @Nullable
    private Object deserializeMapValue(@Nullable Object raw, @Nullable Type valueType) {
        if (raw == null) {
            return null;
        }
        if (valueType == null) {
            // 未声明泛型：section 转 Map 保留结构
            return raw instanceof ConfigurationSection ? sectionToMap((ConfigurationSection) raw) : raw;
        }

        if (valueType instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) valueType;
            Class<?> rawType = (Class<?>) parameterized.getRawType();
            if (List.class.isAssignableFrom(rawType) && raw instanceof List) {
                Type[] args = parameterized.getActualTypeArguments();
                Class<?> elementType = args.length > 0 && args[0] instanceof Class ? (Class<?>) args[0] : String.class;
                List<Object> result = new ArrayList<>();
                for (Object element : (List<?>) raw) {
                    result.add(elementType == String.class ? String.valueOf(element) : convertValue(element, elementType));
                }
                return result;
            }
            return raw;
        }

        Class<?> valueClass = (Class<?>) valueType;
        if (isConfigPojo(valueClass)) {
            return raw instanceof ConfigurationSection
                ? deserializePojo((ConfigurationSection) raw, valueClass)
                : null; // 结构不符，保留 null
        }
        if (raw instanceof ConfigurationSection) {
            return sectionToMap((ConfigurationSection) raw);
        }
        return convertValue(raw, valueClass);
    }

    /**
     * 判断给定类型是否为"配置 POJO"（非标量/集合/Bukkit 类型的普通类，
     * 字段通过 {@code @ConfigValue} 声明相对路径）。
     */
    private boolean isConfigPojo(@NotNull Class<?> type) {
        if (type.isPrimitive() || type.isArray()) return false;
        if (String.class == type || type.isEnum() || Boolean.class == type || Character.class == type) return false;
        if (Number.class.isAssignableFrom(type)) return false;
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) return false;
        if (type.getName().startsWith("java.") || type.getName().startsWith("org.bukkit.")) return false;
        return true;
    }

    /**
     * 实例化配置 POJO 并按 {@code @ConfigValue} 相对路径从 section 填充字段。
     */
    @Nullable
    private Object deserializePojo(@NotNull ConfigurationSection section, @NotNull Class<?> pojoType) {
        try {
            Constructor<?> constructor = pojoType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object pojo = constructor.newInstance();
            fillPojoFromSection(section, pojo);
            return pojo;
        } catch (Exception e) {
            logger.warn("Failed to deserialize configuration POJO {}: {}", pojoType.getName(), e.getMessage());
            return null;
        }
    }

    private void fillPojoFromSection(@NotNull ConfigurationSection section, @NotNull Object pojo) throws IllegalAccessException {
        for (Field field : pojo.getClass().getDeclaredFields()) {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation == null) {
                continue; // 未标注的字段不参与绑定
            }
            String relativePath = annotation.value();
            field.setAccessible(true);

            if (Map.class.isAssignableFrom(field.getType())) {
                if (section.isConfigurationSection(relativePath)) {
                    field.set(pojo, deserializeMap(section.getConfigurationSection(relativePath), field));
                }
                continue;
            }
            // 嵌套 POJO 字段（如按钮的 disabled 子配置）
            if (isConfigPojo(field.getType())) {
                if (section.isConfigurationSection(relativePath)) {
                    field.set(pojo, deserializePojo(section.getConfigurationSection(relativePath), field.getType()));
                }
                continue;
            }
            if (!section.contains(relativePath)) {
                continue;
            }
            setPlainValue(field, pojo, section.get(relativePath));
        }
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  序列化 | Serialization
       └─────────────────────────────────────────────────────────────────┘
     */

    /**
     * 将字段值转换为可写回 YAML 的结构：
     * 枚举转字符串、配置 POJO 转 Map、Map 递归处理、数组与 List 处理元素。
     */
    @Nullable
    private Object serializeFieldValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().isEnum()) {
            return value.toString();
        }
        if (value instanceof ConfigurationSection) {
            return sectionToMap((ConfigurationSection) value);
        }
        if (value instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), serializeFieldValue(entry.getValue()));
            }
            return result;
        }
        if (isConfigPojo(value.getClass())) {
            return serializePojo(value);
        }
        // 数组写成 YAML 列表：直接交给 YAML 会得到 [Ljava.lang.String;@1b6d3586 这种东西
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                result.add(serializeFieldValue(Array.get(value, index)));
            }
            return result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object element : (List<?>) value) {
                result.add(serializeFieldValue(element));
            }
            return result;
        }
        return value;
    }

    /**
     * 将配置 POJO 序列化为 Map（键为 {@code @ConfigValue} 相对路径，支持点分路径）。
     */
    @NotNull
    private Map<String, Object> serializePojo(@NotNull Object pojo) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Field field : pojo.getClass().getDeclaredFields()) {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation == null) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(pojo);
                if (value == null) {
                    continue;
                }
                putDottedPath(result, annotation.value(), serializeFieldValue(value));
            } catch (IllegalAccessException ignored) {
            }
        }
        return result;
    }

    /**
     * 向嵌套 Map 中写入点分路径（"a.b" → {a: {b: value}}）。
     */
    @SuppressWarnings("unchecked")
    private void putDottedPath(@NotNull Map<String, Object> target, @NotNull String path, @Nullable Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = target;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(parts[i], next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }

    /**
     * 将 MemorySection 递归转换为普通 Map（迁移/兜底场景使用）。
     */
    @NotNull
    private Map<String, Object> sectionToMap(@NotNull ConfigurationSection section) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            result.put(key, value instanceof ConfigurationSection ? sectionToMap((ConfigurationSection) value) : value);
        }
        return result;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  注释 | Comments
       └─────────────────────────────────────────────────────────────────┘
     */

    /**
     * 给某个配置键写注释（description 为空则不动）。
     * <p>
     *     两个 Bukkit 怪癖都在这里兜住：它只给列表的<b>第一个</b>元素加 {@code # } 前缀
     *     （所以 {@code \n} 必须由我们先拆成多行，整段交给它除了首行以外都会变成裸文本、
     *     再被 SnakeYAML 折叠回一行）；注释 API 是反射调的，Spigot 1.18.1 以下静默跳过。
     * </p>
     */
    private boolean setComments(@NotNull FileConfiguration config, @NotNull String path, @Nullable String description) {
        if (description == null || description.isEmpty()) {
            return false;
        }
        List<String> lines = new ArrayList<>();
        for (String line : description.split("\n")) {
            // 只用 \n 分段，\r\n 的 \r 会留在行尾（YAML 里是控制字符，不报错但查不出来）
            lines.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
        }
        // TODO: Find a way to support comments on older Spigot versions
        try {
            java.lang.reflect.Method setCommentsMethod = config.getClass().getMethod("setComments", String.class, List.class);
            setCommentsMethod.invoke(config, path, lines);
            return true;
        } catch (Exception ignored) {
            logger.debug("Comments not supported on this server version for field: " + path);
            return false;
        }
    }

    /**
     * 刷新某个键的注释（只有 {@code refreshComment = true} 的字段走这里），返回是否真的改了。
     * <p>
     *     两种情况跳过：注释里有 {@code @keep}（服主写「本服特有约定」的唯一去处），
     *     或现有注释已经与 description 一致——后者让「一致时不写盘」成立，
     *     否则每次启动都重写文件，mtime 一直变，用户会以为配置被动了。
     * </p>
     */
    private boolean refreshCommentIfNeeded(@NotNull FileConfiguration config, @NotNull ConfigurationMetadata.FieldMetadata fieldMeta) {
        if (!fieldMeta.refreshComment || fieldMeta.description.isEmpty()) {
            return false;
        }
        List<String> current;
        try {
            java.lang.reflect.Method getCommentsMethod = config.getClass().getMethod("getComments", String.class);
            Object result = getCommentsMethod.invoke(config, fieldMeta.configPath);
            // 读回来的是「剥掉 # 前缀」的原文、且首元素是 null 占位（Bukkit 的注释列表留了个空位）
            current = new ArrayList<>();
            if (result instanceof List) {
                for (Object line : (List<?>) result) {
                    if (line != null) {
                        current.add(String.valueOf(line));
                    }
                }
            }
        } catch (Exception ignored) {
            return false; // 这个服务端不支持读注释，写入同样不会生效
        }
        for (String line : current) {
            if (line.contains("@keep")) {
                return false;
            }
        }
        List<String> expected = Arrays.asList(fieldMeta.description.split("\n"));
        // \r 要在拼出期望值时就裁掉，否则与读回来的值比不相等（见 setComments 里的说明）
        for (int i = 0; i < expected.size(); i++) {
            String line = expected.get(i);
            if (line.endsWith("\r")) {
                expected.set(i, line.substring(0, line.length() - 1));
            }
        }
        if (current.equals(expected)) {
            return false;
        }
        return setComments(config, fieldMeta.configPath, fieldMeta.description);
    }
}
