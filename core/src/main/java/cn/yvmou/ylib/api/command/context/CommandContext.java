package cn.yvmou.ylib.api.command.context;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 命令执行上下文
 */
public interface CommandContext {
    /**
     * 获取命令发送者
     */
    @NotNull
    CommandSender sender();

    /**
     * 获取已解析的参数值
     */
    @Nullable
    <T> T get(@NotNull String name);

    /**
     * 获取已解析的参数值，如果不存在则返回默认值
     */
    @NotNull
    <T> T getOrDefault(@NotNull String name, @NotNull T defaultValue);

    /**
     * 获取必需的参数值，如果不存在则抛出异常
     */
    @NotNull
    <T> T getRequired(@NotNull String name);

    /**
     * 获取原始参数数组
     */
    @NotNull
    String[] rawArgs();

    /**
     * 获取完整命令字符串
     */
    @NotNull
    String getCommand();
}
