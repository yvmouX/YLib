package cn.yvmou.ylib.impl.config;

import cn.yvmou.ylib.api.config.ConfigurationValidationResult;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult.ValidationError;
import cn.yvmou.ylib.api.config.ConfigurationValidationResult.ValidationWarning;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration validator.
 * Responsible for validating configuration values against metadata.
 */
public class ConfigurationValidator {

    /**
     * 验证配置
     */
    public ConfigurationValidationResult validate(@NotNull Object instance, @NotNull ConfigurationMetadata metadata) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        for (ConfigurationMetadata.FieldMetadata fieldMeta : metadata.fields) {
            try {
                fieldMeta.field.setAccessible(true);
                Object value = fieldMeta.field.get(instance);
                
                // 检查必需字段
                if (fieldMeta.required && (value == null || (value instanceof String && ((String) value).isEmpty()))) {
                    errors.add(new ValidationError(
                        fieldMeta.field.getName(),
                        fieldMeta.configPath,
                        "Required field is missing or empty",
                        value
                    ));
                    continue;
                }
                
                // 验证字段值
                if (value != null && !fieldMeta.validation.isEmpty()) {
                    ValidationResult result = validateFieldValue(value, fieldMeta.validation);
                    if (!result.valid) {
                        errors.add(new ValidationError(
                            fieldMeta.field.getName(),
                            fieldMeta.configPath,
                            result.message,
                            value
                        ));
                    }
                }
                
            } catch (IllegalAccessException e) {
                errors.add(new ValidationError(
                    fieldMeta.field.getName(),
                    fieldMeta.configPath,
                    "Failed to access field: " + e.getMessage(),
                    null
                ));
            }
        }
        
        if (errors.isEmpty()) {
            return warnings.isEmpty() ? 
                ConfigurationValidationResult.success() : 
                ConfigurationValidationResult.withWarnings(warnings);
        } else {
            return ConfigurationValidationResult.failure(errors);
        }
    }

    /**
     * 验证字段值
     */
    private ValidationResult validateFieldValue(Object value, String validation) {
        if (validation.isEmpty()) {
            return new ValidationResult(true, "");
        }
        
        String[] rules = validation.split(",");
        for (String rule : rules) {
            rule = rule.trim();
            
            if (rule.startsWith("min:")) {
                double min = Double.parseDouble(rule.substring(4));
                if (value instanceof Number && ((Number) value).doubleValue() < min) {
                    return new ValidationResult(false, "Value must be at least " + min);
                }
            } else if (rule.startsWith("max:")) {
                double max = Double.parseDouble(rule.substring(4));
                if (value instanceof Number && ((Number) value).doubleValue() > max) {
                    return new ValidationResult(false, "Value must be at most " + max);
                }
            } else if (rule.startsWith("range:")) {
                String range = rule.substring(6);
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0]);
                    double max = Double.parseDouble(parts[1]);
                    if (value instanceof Number) {
                        double val = ((Number) value).doubleValue();
                        if (val < min || val > max) {
                            return new ValidationResult(false, "Value must be between " + min + " and " + max);
                        }
                    }
                }
            } else if (rule.startsWith("length:")) {
                int length = Integer.parseInt(rule.substring(7));
                if (value instanceof String && ((String) value).length() != length) {
                    return new ValidationResult(false, "String length must be " + length);
                }
            } else if (rule.startsWith("regex:")) {
                String pattern = rule.substring(6);
                if (value instanceof String && !Pattern.matches(pattern, (String) value)) {
                    return new ValidationResult(false, "Value does not match pattern: " + pattern);
                }
            } else if (rule.startsWith("enum:")) {
                String enumValues = rule.substring(5);
                String[] allowedValues = enumValues.split("\\|");
                boolean found = false;
                for (String allowedValue : allowedValues) {
                    if (allowedValue.trim().equals(value.toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return new ValidationResult(false, "Value must be one of: " + enumValues);
                }
            }
        }
        
        return new ValidationResult(true, "");
    }
    
    private static class ValidationResult {
        final boolean valid;
        final String message;
        
        ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}
