package cn.yvmou.ylib.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 错误上下文
 * <p>
 * 包含错误发生时的详细上下文信息，帮助开发者快速定位问题。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class ErrorContext {

    private final String component;
    private final String operation;
    private final LocalDateTime timestamp;
    private final Map<String, Object> contextData;
    private final String threadName;
    private final String pluginName;

    /**
     * 构造函数
     *
     * @param component 组件名称
     * @param operation 操作名称
     * @param pluginName 插件名称
     */
    public ErrorContext(@NotNull String component, @NotNull String operation, @Nullable String pluginName) {
        this.component = component;
        this.operation = operation;
        this.pluginName = pluginName;
        this.timestamp = LocalDateTime.now();
        this.contextData = new HashMap<>();
        this.threadName = Thread.currentThread().getName();
    }

    /**
     * 添加上下文数据
     *
     * @param key 键
     * @param value 值
     * @return 当前实例，支持链式调用
     */
    @NotNull
    public ErrorContext addContext(@NotNull String key, @Nullable Object value) {
        contextData.put(key, value);
        return this;
    }

    /**
     * 批量添加上下文数据
     *
     * @param data 上下文数据
     * @return 当前实例，支持链式调用
     */
    @NotNull
    public ErrorContext addContextData(@NotNull Map<String, Object> data) {
        contextData.putAll(data);
        return this;
    }

    /**
     * 获取组件名称
     *
     * @return 组件名称
     */
    @NotNull
    public String getComponent() {
        return component;
    }

    /**
     * 获取操作名称
     *
     * @return 操作名称
     */
    @NotNull
    public String getOperation() {
        return operation;
    }

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    @NotNull
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 获取上下文数据
     *
     * @return 上下文数据
     */
    @NotNull
    public Map<String, Object> getContextData() {
        return new HashMap<>(contextData);
    }

    /**
     * 获取线程名称
     *
     * @return 线程名称
     */
    @NotNull
    public String getThreadName() {
        return threadName;
    }

    /**
     * 获取插件名称
     *
     * @return 插件名称，可能为null
     */
    @Nullable
    public String getPluginName() {
        return pluginName;
    }

    /**
     * 获取上下文数据值
     *
     * @param key 键
     * @return 值，如果不存在则返回null
     */
    @Nullable
    public Object getContextValue(@NotNull String key) {
        return contextData.get(key);
    }

    /**
     * 获取上下文数据值（类型安全）
     *
     * @param <T> 值类型
     * @param key 键
     * @param type 值类型
     * @return 值，如果不存在或类型不匹配则返回null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(@NotNull String key, @NotNull Class<T> type) {
        Object value = contextData.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ErrorContext{");
        sb.append("component='").append(component).append('\'');
        sb.append(", operation='").append(operation).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", thread='").append(threadName).append('\'');
        if (pluginName != null) {
            sb.append(", plugin='").append(pluginName).append('\'');
        }
        if (!contextData.isEmpty()) {
            sb.append(", context=").append(contextData);
        }
        sb.append('}');
        return sb.toString();
    }
}