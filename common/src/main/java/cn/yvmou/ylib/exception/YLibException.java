package cn.yvmou.ylib.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * YLib 异常基类
 * <p>
 * 这个 YLib 的根异常，其他异常都继承自这个异常类
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public class YLibException extends RuntimeException {
    /**
     * 构造函数
     *
     * @param message 错误消息
     */
    public YLibException(@NotNull String message) {
        this(message, null);
    }

    /**
     * 构造函数
     *
     * @param message 错误消息
     * @param cause 原因
     */
    public YLibException(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
