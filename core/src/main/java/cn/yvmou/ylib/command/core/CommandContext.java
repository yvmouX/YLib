package cn.yvmou.ylib.command.core;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class CommandContext {
    private final CommandSender sender;
    private final Map<String, Object> arguments;
    private final String[] rawArgs;
    private final String command;

    /**
     * 命令执行上下文
     */
    public CommandContext(CommandSender sender, Map<String, Object> arguments, String[] rawArgs, String command) {
        this.sender = sender;
        this.arguments = arguments != null ? arguments : new HashMap<>();
        this.rawArgs = rawArgs;
        this.command = command;
    }

    /**
     * 获取命令发送者
     */
    public @NotNull CommandSender sender() {
        return sender;
    }

    /**
     * 获取已解析的参数值
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(@NotNull String name) {
        return (T) arguments.get(name);
    }

    /**
     * 获取已解析的参数值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getOrDefault(@NotNull String name, @NotNull T defaultValue) {
        Object value = arguments.get(name);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 获取必需的参数值，如果不存在则抛出异常
     */
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getRequired(@NotNull String name) {
        Object value = arguments.get(name);
        if (value == null) {
            throw new NoSuchElementException("Missing required argument: " + name);
        }
        return (T) value;
    }

    /**
     * 获取原始参数数组
     */
    public @NotNull String[] rawArgs() {
        return rawArgs;
    }

    /**
     * 获取完整命令字符串
     */
    public @NotNull String getCommand() {
        return command;
    }
}
