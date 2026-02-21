package cn.yvmou.ylib.api.command.args;

import cn.yvmou.ylib.api.command.context.CommandContext;
import cn.yvmou.ylib.api.command.exception.CommandValidationException;
import org.jetbrains.annotations.NotNull;

/**
 * 参数验证器接口
 * @param <T> 参数类型
 */
@FunctionalInterface
public interface ArgumentValidator<T> {
    /**
     * 验证参数
     * @param context 上下文
     * @param value 解析后的值
     * @throws CommandValidationException 验证失败时抛出
     */
    void validate(@NotNull CommandContext context, @NotNull T value) throws CommandValidationException;
}
