package cn.yvmou.ylib.api.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置验证结果
 * <p>
 * 包含配置验证的结果信息，包括是否通过验证以及具体的错误信息。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class ConfigurationValidationResult {
    
    private final boolean valid;
    private final List<ValidationError> errors;
    private final List<ValidationWarning> warnings;
    
    /**
     * 创建成功的验证结果
     * 
     * @return 成功的验证结果
     */
    @NotNull
    public static ConfigurationValidationResult success() {
        return new ConfigurationValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * 创建失败的验证结果
     * 
     * @param errors 错误列表
     * @return 失败的验证结果
     */
    @NotNull
    public static ConfigurationValidationResult failure(@NotNull List<ValidationError> errors) {
        return new ConfigurationValidationResult(false, errors, Collections.emptyList());
    }
    
    /**
     * 创建带警告的验证结果
     * 
     * @param warnings 警告列表
     * @return 带警告的验证结果
     */
    @NotNull
    public static ConfigurationValidationResult withWarnings(@NotNull List<ValidationWarning> warnings) {
        return new ConfigurationValidationResult(true, Collections.emptyList(), warnings);
    }
    
    /**
     * 构造函数
     * 
     * @param valid 是否验证通过
     * @param errors 错误列表
     * @param warnings 警告列表
     */
    public ConfigurationValidationResult(boolean valid, @NotNull List<ValidationError> errors, @NotNull List<ValidationWarning> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }
    
    /**
     * 是否验证通过
     * 
     * @return 如果验证通过返回true，否则返回false
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * 获取错误列表
     * 
     * @return 错误列表
     */
    @NotNull
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * 获取警告列表
     * 
     * @return 警告列表
     */
    @NotNull
    public List<ValidationWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    /**
     * 是否有错误
     * 
     * @return 如果有错误返回true，否则返回false
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * 是否有警告
     * 
     * @return 如果有警告返回true，否则返回false
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * 获取错误数量
     * 
     * @return 错误数量
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * 获取警告数量
     * 
     * @return 警告数量
     */
    public int getWarningCount() {
        return warnings.size();
    }
    
    @Override
    public String toString() {
        if (valid && warnings.isEmpty()) {
            return "Validation passed";
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (!valid) {
            sb.append("Validation failed with ").append(errors.size()).append(" error(s)");
            if (!warnings.isEmpty()) {
                sb.append(" and ").append(warnings.size()).append(" warning(s)");
            }
        } else {
            sb.append("Validation passed with ").append(warnings.size()).append(" warning(s)");
        }
        
        return sb.toString();
    }
    
    /**
     * 验证错误
     */
    public static class ValidationError {
        private final String fieldName;
        private final String configPath;
        private final String message;
        private final Object actualValue;
        
        /**
         * 构造函数
         * 
         * @param fieldName 字段名称
         * @param configPath 配置路径
         * @param message 错误消息
         * @param actualValue 实际值
         */
        public ValidationError(@NotNull String fieldName, @NotNull String configPath, @NotNull String message, Object actualValue) {
            this.fieldName = fieldName;
            this.configPath = configPath;
            this.message = message;
            this.actualValue = actualValue;
        }
        
        /**
         * 获取字段名称
         * 
         * @return 字段名称
         */
        @NotNull
        public String getFieldName() {
            return fieldName;
        }
        
        /**
         * 获取配置路径
         * 
         * @return 配置路径
         */
        @NotNull
        public String getConfigPath() {
            return configPath;
        }
        
        /**
         * 获取错误消息
         * 
         * @return 错误消息
         */
        @NotNull
        public String getMessage() {
            return message;
        }
        
        /**
         * 获取实际值
         * 
         * @return 实际值
         */
        public Object getActualValue() {
            return actualValue;
        }
        
        @Override
        public String toString() {
            return String.format("Field '%s' (path: %s): %s (actual value: %s)", 
                fieldName, configPath, message, actualValue);
        }
    }
    
    /**
     * 验证警告
     */
    public static class ValidationWarning {
        private final String fieldName;
        private final String configPath;
        private final String message;
        
        /**
         * 构造函数
         * 
         * @param fieldName 字段名称
         * @param configPath 配置路径
         * @param message 警告消息
         */
        public ValidationWarning(@NotNull String fieldName, @NotNull String configPath, @NotNull String message) {
            this.fieldName = fieldName;
            this.configPath = configPath;
            this.message = message;
        }
        
        /**
         * 获取字段名称
         * 
         * @return 字段名称
         */
        @NotNull
        public String getFieldName() {
            return fieldName;
        }
        
        /**
         * 获取配置路径
         * 
         * @return 配置路径
         */
        @NotNull
        public String getConfigPath() {
            return configPath;
        }
        
        /**
         * 获取警告消息
         * 
         * @return 警告消息
         */
        @NotNull
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("Field '%s' (path: %s): %s", fieldName, configPath, message);
        }
    }
}