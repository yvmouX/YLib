package cn.yvmou.ylib.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * YLib自定义异常基类
 * <p>
 * 增强的异常类，包含错误上下文、用户友好消息、恢复建议等信息。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class YLibException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** 错误上下文 */
    private final ErrorContext errorContext;
    /** 用户友好消息 */
    private final String userFriendlyMessage;
    /** 恢复建议列表 */
    private final List<String> recoverySuggestions;
    /** 错误严重程度 */
    private final ErrorSeverity severity;
    /** 错误代码 */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public YLibException(@NotNull String message) {
        this(message, null, null, null, ErrorSeverity.ERROR, null);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public YLibException(@NotNull String message, @Nullable Throwable cause) {
        this(message, cause, null, null, ErrorSeverity.ERROR, null);
    }

    /**
     * 构造函数
     *
     * @param cause 原因
     */
    public YLibException(@NotNull Throwable cause) {
        this(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName(),
                cause, null, null, ErrorSeverity.ERROR, null);
    }

    /**
     * 完整构造函数
     *
     * @param message 错误消息
     * @param cause 原因
     * @param errorContext 错误上下文
     * @param userFriendlyMessage 用户友好消息
     * @param severity 错误严重程度
     * @param errorCode 错误代码
     */
    public YLibException(@NotNull String message, @Nullable Throwable cause,
                         @Nullable ErrorContext errorContext, @Nullable String userFriendlyMessage,
                         @NotNull ErrorSeverity severity, @Nullable String errorCode) {
        super(message, cause);
        this.errorContext = errorContext;
        this.userFriendlyMessage = userFriendlyMessage;
        this.recoverySuggestions = new ArrayList<>();
        this.severity = severity;
        this.errorCode = errorCode;
    }

    /**
     * 获取错误上下文
     *
     * @return 错误上下文，可能为null
     */
    @Nullable
    public ErrorContext getErrorContext() {
        return errorContext;
    }

    /**
     * 获取用户友好消息
     *
     * @return 用户友好消息，如果未设置则返回原始消息
     */
    @NotNull
    public String getUserFriendlyMessage() {
        return userFriendlyMessage != null ? userFriendlyMessage : getMessage();
    }

    /**
     * 获取恢复建议
     *
     * @return 恢复建议列表
     */
    @NotNull
    public List<String> getRecoverySuggestions() {
        return new ArrayList<>(recoverySuggestions);
    }

    /**
     * 添加恢复建议
     *
     * @param suggestion 恢复建议
     * @return 当前实例，支持链式调用
     */
    @NotNull
    public YLibException addRecoverySuggestion(@NotNull String suggestion) {
        recoverySuggestions.add(suggestion);
        return this;
    }

    /**
     * 获取错误严重程度
     *
     * @return 错误严重程度
     */
    @NotNull
    public ErrorSeverity getSeverity() {
        return severity;
    }

    /**
     * 获取错误代码
     *
     * @return 错误代码，可能为null
     */
    @Nullable
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 检查是否有用户友好消息
     *
     * @return 如果有用户友好消息返回true，否则返回false
     */
    public boolean hasUserFriendlyMessage() {
        return userFriendlyMessage != null;
    }

    /**
     * 检查是否有恢复建议
     *
     * @return 如果有恢复建议返回true，否则返回false
     */
    public boolean hasRecoverySuggestions() {
        return !recoverySuggestions.isEmpty();
    }

    /**
     * 检查是否有错误上下文
     *
     * @return 如果有错误上下文返回true，否则返回false
     */
    public boolean hasErrorContext() {
        return errorContext != null;
    }

    /**
     * 生成详细的错误报告
     *
     * @return 详细的错误报告
     */
    @NotNull
    public String generateDetailedReport() {
        StringBuilder sb = new StringBuilder();

        // 基本信息
        sb.append("=== YLib错误报告 ===\n");
        if (errorCode != null) {
            sb.append("错误代码: ").append(errorCode).append("\n");
        }
        sb.append("严重程度: ").append(severity.getDisplayName()).append("\n");
        sb.append("错误消息: ").append(getMessage()).append("\n");

        // 用户友好消息
        if (hasUserFriendlyMessage()) {
            sb.append("用户消息: ").append(getUserFriendlyMessage()).append("\n");
        }

        // 错误上下文
        if (hasErrorContext()) {
            sb.append("错误上下文: ").append(errorContext.toString()).append("\n");
        }

        // 恢复建议
        if (hasRecoverySuggestions()) {
            sb.append("恢复建议:\n");
            for (int i = 0; i < recoverySuggestions.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(recoverySuggestions.get(i)).append("\n");
            }
        }

        // 原因链
        if (getCause() != null) {
            sb.append("原因: ").append(getCause().getMessage()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 错误严重程度枚举
     */
    public enum ErrorSeverity {
        /** 信息级别 */
        INFO("信息", 0),
        /** 警告级别 */
        WARNING("警告", 1),
        /** 错误级别 */
        ERROR("错误", 2),
        /** 严重级别 */
        CRITICAL("严重", 3),
        /** 致命级别 */
        FATAL("致命", 4);

        private final String displayName;
        private final int level;

        ErrorSeverity(String displayName, int level) {
            this.displayName = displayName;
            this.level = level;
        }

        /**
         * 获取显示名称
         *
         * @return 显示名称
         */
        @NotNull
        public String getDisplayName() {
            return displayName;
        }

        /**
         * 获取严重程度级别
         *
         * @return 严重程度级别
         */
        public int getLevel() {
            return level;
        }

        /**
         * 检查是否比另一个严重程度更严重
         *
         * @param other 另一个严重程度
         * @return 如果更严重返回true
         */
        public boolean isMoreSevereThan(@NotNull ErrorSeverity other) {
            return this.level > other.level;
        }
    }
}
