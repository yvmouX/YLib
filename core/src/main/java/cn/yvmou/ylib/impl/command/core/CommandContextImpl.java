package cn.yvmou.ylib.impl.command.core;

import cn.yvmou.ylib.api.command.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class CommandContextImpl implements CommandContext {
    private final CommandSender sender;
    private final Map<String, Object> arguments;
    private final String[] rawArgs;
    private final String command;

    public CommandContextImpl(CommandSender sender, Map<String, Object> arguments, String[] rawArgs, String command) {
        this.sender = sender;
        this.arguments = arguments != null ? arguments : new HashMap<>();
        this.rawArgs = rawArgs;
        this.command = command;
    }

    @Override
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(@NotNull String name) {
        return (T) arguments.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getOrDefault(@NotNull String name, @NotNull T defaultValue) {
        Object value = arguments.get(name);
        return value != null ? (T) value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull <T> T getRequired(@NotNull String name) {
        Object value = arguments.get(name);
        if (value == null) {
            throw new NoSuchElementException("Missing required argument: " + name);
        }
        return (T) value;
    }

    @Override
    public @NotNull String[] rawArgs() {
        return rawArgs;
    }

    @Override
    public @NotNull String getCommand() {
        return command;
    }
}
