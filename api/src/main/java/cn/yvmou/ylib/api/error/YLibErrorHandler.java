package cn.yvmou.ylib.api.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * YLib错误处理器接口
 * <p>
 * 提供统一的错误处理机制，包括错误记录、用户友好的错误消息生成、
 * 错误恢复建议等功能。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface YLibErrorHandler {
    
    /**
     * 处理错误
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @return 错误处理结果
     */
    @NotNull
    ErrorHandlingResult handleError(@NotNull Throwable error, @NotNull ErrorContext context);
    
    /**
     * 生成用户友好的错误消息
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @return 用户友好的错误消息
     */
    @NotNull
    String generateUserFriendlyMessage(@NotNull Throwable error, @NotNull ErrorContext context);
    
    /**
     * 生成错误恢复建议
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @return 错误恢复建议列表
     */
    @NotNull
    java.util.List<String> generateRecoverySuggestions(@NotNull Throwable error, @NotNull ErrorContext context);
    
    /**
     * 记录错误
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @param userMessage 用户友好消息
     */
    void logError(@NotNull Throwable error, @NotNull ErrorContext context, @NotNull String userMessage);
    
    /**
     * 检查错误是否可恢复
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @return 如果错误可恢复返回true，否则返回false
     */
    boolean isRecoverable(@NotNull Throwable error, @NotNull ErrorContext context);
    
    /**
     * 尝试自动恢复错误
     * 
     * @param error 错误对象
     * @param context 错误上下文
     * @return 恢复结果
     */
    @NotNull
    RecoveryResult attemptRecovery(@NotNull Throwable error, @NotNull ErrorContext context);
    
    /**
     * 添加错误监听器
     * 
     * @param listener 错误监听器
     */
    void addErrorListener(@NotNull ErrorListener listener);
    
    /**
     * 移除错误监听器
     * 
     * @param listener 错误监听器
     */
    void removeErrorListener(@NotNull ErrorListener listener);
    
    /**
     * 获取错误统计信息
     * 
     * @return 错误统计信息
     */
    @NotNull
    ErrorStatistics getErrorStatistics();
    
    /**
     * 错误处理结果
     */
    class ErrorHandlingResult {
        private final boolean handled;
        private final String userMessage;
        private final java.util.List<String> suggestions;
        private final boolean shouldContinue;
        
        /**
         * 构造函数
         * 
         * @param handled 是否已处理
         * @param userMessage 用户友好消息
         * @param suggestions 恢复建议列表
         * @param shouldContinue 是否应该继续运行
         */
        public ErrorHandlingResult(boolean handled, @NotNull String userMessage, 
                                 @NotNull java.util.List<String> suggestions, boolean shouldContinue) {
            this.handled = handled;
            this.userMessage = userMessage;
            this.suggestions = suggestions;
            this.shouldContinue = shouldContinue;
        }
        
        /**
         * 检查错误是否已处理
         * 
         * @return 如果已处理返回true
         */
        public boolean isHandled() { return handled; }
        
        /**
         * 获取用户友好消息
         * 
         * @return 用户友好消息
         */
        @NotNull public String getUserMessage() { return userMessage; }
        
        /**
         * 获取恢复建议列表
         * 
         * @return 恢复建议列表
         */
        @NotNull public java.util.List<String> getSuggestions() { return suggestions; }
        
        /**
         * 检查是否应该继续运行
         * 
         * @return 如果应该继续返回true
         */
        public boolean shouldContinue() { return shouldContinue; }
    }
    
    /**
     * 恢复结果
     */
    class RecoveryResult {
        private final boolean successful;
        private final String message;
        private final Exception recoveryError;
        
        /**
         * 构造函数
         * 
         * @param successful 是否成功
         * @param message 恢复消息
         * @param recoveryError 恢复过程中的错误
         */
        public RecoveryResult(boolean successful, @NotNull String message, @Nullable Exception recoveryError) {
            this.successful = successful;
            this.message = message;
            this.recoveryError = recoveryError;
        }
        
        /**
         * 检查恢复是否成功
         * 
         * @return 如果成功返回true
         */
        public boolean isSuccessful() { return successful; }
        
        /**
         * 获取恢复消息
         * 
         * @return 恢复消息
         */
        @NotNull public String getMessage() { return message; }
        
        /**
         * 获取恢复过程中的错误
         * 
         * @return 恢复错误，可能为null
         */
        @Nullable public Exception getRecoveryError() { return recoveryError; }
        
        /**
         * 创建成功的恢复结果
         * 
         * @param message 成功消息
         * @return 成功的恢复结果
         */
        @NotNull
        public static RecoveryResult success(@NotNull String message) {
            return new RecoveryResult(true, message, null);
        }
        
        /**
         * 创建失败的恢复结果
         * 
         * @param message 失败消息
         * @param error 失败原因
         * @return 失败的恢复结果
         */
        @NotNull
        public static RecoveryResult failure(@NotNull String message, @Nullable Exception error) {
            return new RecoveryResult(false, message, error);
        }
    }
    
    /**
     * 错误监听器
     */
    @FunctionalInterface
    interface ErrorListener {
        /**
         * 错误发生时调用
         * 
         * @param error 错误对象
         * @param context 错误上下文
         * @param result 处理结果
         */
        void onError(@NotNull Throwable error, @NotNull ErrorContext context, @NotNull ErrorHandlingResult result);
    }
    
    /**
     * 错误统计信息
     */
    class ErrorStatistics {
        private final int totalErrors;
        private final int recoveredErrors;
        private final java.util.Map<String, Integer> errorsByComponent;
        private final java.util.Map<Class<? extends Throwable>, Integer> errorsByType;
        
        /**
         * 构造函数
         * 
         * @param totalErrors 总错误数
         * @param recoveredErrors 已恢复错误数
         * @param errorsByComponent 按组件分类的错误统计
         * @param errorsByType 按类型分类的错误统计
         */
        public ErrorStatistics(int totalErrors, int recoveredErrors,
                             @NotNull java.util.Map<String, Integer> errorsByComponent,
                             @NotNull java.util.Map<Class<? extends Throwable>, Integer> errorsByType) {
            this.totalErrors = totalErrors;
            this.recoveredErrors = recoveredErrors;
            this.errorsByComponent = errorsByComponent;
            this.errorsByType = errorsByType;
        }
        
        /**
         * 获取总错误数
         * 
         * @return 总错误数
         */
        public int getTotalErrors() { return totalErrors; }
        
        /**
         * 获取已恢复错误数
         * 
         * @return 已恢复错误数
         */
        public int getRecoveredErrors() { return recoveredErrors; }
        
        /**
         * 获取错误恢复率
         * 
         * @return 错误恢复率（0.0-1.0）
         */
        public double getRecoveryRate() { 
            return totalErrors > 0 ? (double) recoveredErrors / totalErrors : 0.0; 
        }
        
        /**
         * 获取按组件分类的错误统计
         * 
         * @return 按组件分类的错误统计
         */
        @NotNull public java.util.Map<String, Integer> getErrorsByComponent() { return errorsByComponent; }
        
        /**
         * 获取按类型分类的错误统计
         * 
         * @return 按类型分类的错误统计
         */
        @NotNull public java.util.Map<Class<? extends Throwable>, Integer> getErrorsByType() { return errorsByType; }
    }
}