package cn.yvmou.ylib.common.error;

import cn.yvmou.ylib.api.container.ServiceNotFoundException;
import cn.yvmou.ylib.api.config.ConfigurationException;
import cn.yvmou.ylib.api.error.ErrorContext;
import cn.yvmou.ylib.api.error.YLibErrorHandler;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.api.services.LoggerService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简单的YLib错误处理器实现
 * <p>
 * 提供基础的错误处理功能，包括错误分类、用户友好消息生成、
 * 恢复建议提供等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class SimpleYLibErrorHandler implements YLibErrorHandler {
    
    private final LoggerService logger;
    private final List<ErrorListener> errorListeners = new CopyOnWriteArrayList<>();
    
    // 错误统计
    private volatile int totalErrors = 0;
    private volatile int recoveredErrors = 0;
    private final Map<String, Integer> errorsByComponent = new ConcurrentHashMap<>();
    private final Map<Class<? extends Throwable>, Integer> errorsByType = new ConcurrentHashMap<>();
    
    // 错误消息模板
    private final Map<Class<? extends Throwable>, String> errorMessageTemplates = new HashMap<>();
    
    // 恢复策略
    private final Map<Class<? extends Throwable>, RecoveryStrategy> recoveryStrategies = new HashMap<>();
    
    /**
     * 构造函数
     * 
     * @param logger 日志服务
     */
    public SimpleYLibErrorHandler(@NotNull LoggerService logger) {
        this.logger = logger;
        initializeErrorTemplates();
        initializeRecoveryStrategies();
    }
    
    @Override
    @NotNull
    public ErrorHandlingResult handleError(@NotNull Throwable error, @NotNull ErrorContext context) {
        // 更新统计信息
        updateStatistics(error, context);
        
        // 生成用户友好消息
        String userMessage = generateUserFriendlyMessage(error, context);
        
        // 生成恢复建议
        List<String> suggestions = generateRecoverySuggestions(error, context);
        
        // 记录错误
        logError(error, context, userMessage);
        
        // 尝试自动恢复
        boolean shouldContinue = true;
        if (isRecoverable(error, context)) {
            RecoveryResult recoveryResult = attemptRecovery(error, context);
            if (recoveryResult.isSuccessful()) {
                recoveredErrors++;
                logger.info("错误已自动恢复: " + recoveryResult.getMessage());
            } else {
                shouldContinue = !isCriticalError(error);
            }
        } else {
            shouldContinue = !isCriticalError(error);
        }
        
        // 创建处理结果
        ErrorHandlingResult result = new ErrorHandlingResult(true, userMessage, suggestions, shouldContinue);
        
        // 通知监听器
        notifyErrorListeners(error, context, result);
        
        return result;
    }
    
    @Override
    @NotNull
    public String generateUserFriendlyMessage(@NotNull Throwable error, @NotNull ErrorContext context) {
        // 如果是YLib异常且有用户友好消息，直接返回
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            if (ylibError.hasUserFriendlyMessage()) {
                return ylibError.getUserFriendlyMessage();
            }
        }
        
        // 根据错误类型生成友好消息
        String template = errorMessageTemplates.get(error.getClass());
        if (template != null) {
            return formatErrorMessage(template, error, context);
        }
        
        // 根据错误类别生成通用友好消息
        if (error instanceof ConfigurationException) {
            return generateConfigurationErrorMessage((ConfigurationException) error, context);
        } else if (error instanceof ServiceNotFoundException) {
            return generateServiceNotFoundMessage((ServiceNotFoundException) error, context);
        } else if (error instanceof IOException) {
            return generateIOErrorMessage((IOException) error, context);
        } else if (error instanceof IllegalArgumentException) {
            return generateArgumentErrorMessage((IllegalArgumentException) error, context);
        } else if (error instanceof NullPointerException) {
            return generateNullPointerMessage((NullPointerException) error, context);
        }
        
        // 默认消息
        return String.format("在%s执行%s时发生了错误: %s", 
            context.getComponent(), context.getOperation(), 
            error.getMessage() != null ? error.getMessage() : "未知错误");
    }
    
    @Override
    @NotNull
    public List<String> generateRecoverySuggestions(@NotNull Throwable error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        
        // 如果是YLib异常且有恢复建议，先添加这些建议
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            suggestions.addAll(ylibError.getRecoverySuggestions());
        }
        
        // 根据错误类型添加通用建议
        if (error instanceof ConfigurationException) {
            suggestions.addAll(getConfigurationErrorSuggestions((ConfigurationException) error, context));
        } else if (error instanceof ServiceNotFoundException) {
            suggestions.addAll(getServiceNotFoundSuggestions((ServiceNotFoundException) error, context));
        } else if (error instanceof IOException) {
            suggestions.addAll(getIOErrorSuggestions((IOException) error, context));
        } else if (error instanceof IllegalArgumentException) {
            suggestions.addAll(getArgumentErrorSuggestions((IllegalArgumentException) error, context));
        } else if (error instanceof NullPointerException) {
            suggestions.addAll(getNullPointerSuggestions((NullPointerException) error, context));
        }
        
        // 添加通用建议
        if (suggestions.isEmpty()) {
            suggestions.add("检查相关配置是否正确");
            suggestions.add("查看完整的错误日志以获取更多信息");
            suggestions.add("如果问题持续存在，请联系插件开发者");
        }
        
        return suggestions;
    }
    
    @Override
    public void logError(@NotNull Throwable error, @NotNull ErrorContext context, @NotNull String userMessage) {
        // 根据错误严重程度选择日志级别
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            switch (ylibError.getSeverity()) {
                case INFO:
                    logger.info(userMessage);
                    break;
                case WARNING:
                    logger.warning(userMessage);
                    break;
                case ERROR:
                    logger.error(userMessage);
                    break;
                case CRITICAL:
                case FATAL:
                    logger.severe("严重错误: " + userMessage);
                    break;
            }
        } else {
            logger.error(userMessage);
        }
        
        // 记录详细的错误信息
        logger.debug("错误详情: " + error.getClass().getName() + " - " + error.getMessage());
        logger.debug("错误上下文: " + context.toString());
        
        // 如果是严重错误，记录堆栈跟踪
        if (isCriticalError(error)) {
            logger.severe("堆栈跟踪: " + error.getClass().getName() + " - " + error.getMessage());
            logger.error("详细错误信息", error);
        }
    }
    
    @Override
    public boolean isRecoverable(@NotNull Throwable error, @NotNull ErrorContext context) {
        // YLib异常的可恢复性判断
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            return ylibError.getSeverity().getLevel() < YLibException.ErrorSeverity.CRITICAL.getLevel();
        }
        
        // 根据错误类型判断
        return recoveryStrategies.containsKey(error.getClass()) ||
               error instanceof ConfigurationException ||
               error instanceof ServiceNotFoundException ||
               error instanceof IllegalArgumentException;
    }
    
    @Override
    @NotNull
    public RecoveryResult attemptRecovery(@NotNull Throwable error, @NotNull ErrorContext context) {
        RecoveryStrategy strategy = recoveryStrategies.get(error.getClass());
        if (strategy != null) {
            try {
                return strategy.recover(error, context);
            } catch (Exception e) {
                return RecoveryResult.failure("自动恢复失败: " + e.getMessage(), e);
            }
        }
        
        // 默认恢复策略
        if (error instanceof ConfigurationException) {
            return attemptConfigurationRecovery((ConfigurationException) error, context);
        } else if (error instanceof ServiceNotFoundException) {
            return attemptServiceRecovery((ServiceNotFoundException) error, context);
        }
        
        return RecoveryResult.failure("没有可用的恢复策略", null);
    }
    
    @Override
    public void addErrorListener(@NotNull ErrorListener listener) {
        errorListeners.add(listener);
    }
    
    @Override
    public void removeErrorListener(@NotNull ErrorListener listener) {
        errorListeners.remove(listener);
    }
    
    @Override
    @NotNull
    public ErrorStatistics getErrorStatistics() {
        return new ErrorStatistics(
            totalErrors,
            recoveredErrors,
            new HashMap<>(errorsByComponent),
            new HashMap<>(errorsByType)
        );
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 初始化错误消息模板
     */
    private void initializeErrorTemplates() {
        errorMessageTemplates.put(NullPointerException.class, 
            "在{component}的{operation}操作中遇到了空值错误");
        errorMessageTemplates.put(IllegalArgumentException.class, 
            "在{component}的{operation}操作中传入了无效的参数");
        errorMessageTemplates.put(IOException.class, 
            "在{component}的{operation}操作中发生了文件读写错误");
    }
    
    /**
     * 初始化恢复策略
     */
    private void initializeRecoveryStrategies() {
        // 配置错误恢复策略
        recoveryStrategies.put(ConfigurationException.class, (error, context) -> {
            // 尝试使用默认配置
            return RecoveryResult.success("使用默认配置值继续运行");
        });
        
        // 服务未找到恢复策略
        recoveryStrategies.put(ServiceNotFoundException.class, (error, context) -> {
            // 尝试使用备用服务或默认实现
            return RecoveryResult.failure("无法找到备用服务", null);
        });
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(@NotNull Throwable error, @NotNull ErrorContext context) {
        totalErrors++;
        
        // 按组件统计
        errorsByComponent.merge(context.getComponent(), 1, Integer::sum);
        
        // 按类型统计
        errorsByType.merge(error.getClass(), 1, Integer::sum);
    }
    
    /**
     * 判断是否为严重错误
     */
    private boolean isCriticalError(@NotNull Throwable error) {
        if (error instanceof YLibException) {
            YLibException ylibError = (YLibException) error;
            return ylibError.getSeverity().isMoreSevereThan(YLibException.ErrorSeverity.ERROR);
        }
        
        return error instanceof OutOfMemoryError ||
               error instanceof StackOverflowError ||
               error instanceof NoClassDefFoundError;
    }
    
    /**
     * 格式化错误消息
     */
    private String formatErrorMessage(@NotNull String template, @NotNull Throwable error, @NotNull ErrorContext context) {
        return template.replace("{component}", context.getComponent())
                      .replace("{operation}", context.getOperation())
                      .replace("{error}", error.getMessage() != null ? error.getMessage() : "未知错误");
    }
    
    /**
     * 生成配置错误消息
     */
    private String generateConfigurationErrorMessage(@NotNull ConfigurationException error, @NotNull ErrorContext context) {
        if (error.getConfigurationClass() != null) {
            return String.format("配置类 %s 出现错误: %s", 
                error.getConfigurationClass().getSimpleName(), error.getMessage());
        }
        return "配置处理出现错误: " + error.getMessage();
    }
    
    /**
     * 生成服务未找到消息
     */
    private String generateServiceNotFoundMessage(@NotNull ServiceNotFoundException error, @NotNull ErrorContext context) {
        return String.format("无法找到所需的服务: %s。请确保服务已正确注册。", 
            error.getServiceClass().getSimpleName());
    }
    
    /**
     * 生成IO错误消息
     */
    private String generateIOErrorMessage(@NotNull IOException error, @NotNull ErrorContext context) {
        return String.format("文件操作失败: %s。请检查文件权限和磁盘空间。", error.getMessage());
    }
    
    /**
     * 生成参数错误消息
     */
    private String generateArgumentErrorMessage(@NotNull IllegalArgumentException error, @NotNull ErrorContext context) {
        return String.format("参数错误: %s。请检查传入的参数是否正确。", error.getMessage());
    }
    
    /**
     * 生成空指针消息
     */
    private String generateNullPointerMessage(@NotNull NullPointerException error, @NotNull ErrorContext context) {
        return String.format("在%s中遇到空值错误。请检查相关对象是否已正确初始化。", context.getOperation());
    }
    
    // 恢复建议生成方法
    private List<String> getConfigurationErrorSuggestions(@NotNull ConfigurationException error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("检查配置文件格式是否正确");
        suggestions.add("验证配置文件中的值是否符合要求");
        suggestions.add("尝试删除配置文件以重新生成默认配置");
        if (error.getConfigPath() != null) {
            suggestions.add("检查配置路径 '" + error.getConfigPath() + "' 的值");
        }
        return suggestions;
    }
    
    private List<String> getServiceNotFoundSuggestions(@NotNull ServiceNotFoundException error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("确保服务 " + error.getServiceClass().getSimpleName() + " 已注册到服务容器");
        suggestions.add("检查服务注册的时机是否正确");
        suggestions.add("验证服务的依赖关系是否满足");
        return suggestions;
    }
    
    private List<String> getIOErrorSuggestions(@NotNull IOException error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("检查文件路径是否正确");
        suggestions.add("验证文件权限设置");
        suggestions.add("确保磁盘空间充足");
        suggestions.add("检查文件是否被其他程序占用");
        return suggestions;
    }
    
    private List<String> getArgumentErrorSuggestions(@NotNull IllegalArgumentException error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("检查传入的参数值是否在有效范围内");
        suggestions.add("验证参数类型是否正确");
        suggestions.add("确保必需的参数不为空");
        return suggestions;
    }
    
    private List<String> getNullPointerSuggestions(@NotNull NullPointerException error, @NotNull ErrorContext context) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("检查相关对象是否已正确初始化");
        suggestions.add("添加空值检查");
        suggestions.add("确保在使用前已创建对象实例");
        return suggestions;
    }
    
    // 恢复策略方法
    private RecoveryResult attemptConfigurationRecovery(@NotNull ConfigurationException error, @NotNull ErrorContext context) {
        // 简单的配置恢复策略：使用默认值
        return RecoveryResult.success("已使用默认配置值，请检查配置文件");
    }
    
    private RecoveryResult attemptServiceRecovery(@NotNull ServiceNotFoundException error, @NotNull ErrorContext context) {
        // 服务恢复策略：提示注册服务
        return RecoveryResult.failure("请确保服务已正确注册", null);
    }
    
    /**
     * 通知错误监听器
     */
    private void notifyErrorListeners(@NotNull Throwable error, @NotNull ErrorContext context, @NotNull ErrorHandlingResult result) {
        for (ErrorListener listener : errorListeners) {
            try {
                listener.onError(error, context, result);
            } catch (Exception e) {
                logger.warning("错误监听器执行失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 恢复策略接口
     */
    @FunctionalInterface
    private interface RecoveryStrategy {
        RecoveryResult recover(Throwable error, ErrorContext context) throws Exception;
    }
}