package cn.yvmou.ylib.impl.services;

import cn.yvmou.ylib.api.services.LoggerService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

public class LoggerServiceImpl implements LoggerService {
    private final JavaPlugin plugin;

    public LoggerServiceImpl(@NotNull final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getConsolePrefix() {
        String prefix = plugin.getDescription().getPrefix();
        String pluginName = (prefix == null || prefix.isEmpty()) ? "UNKNOWN" : prefix;
        return "§8[§b§l§n" + pluginName + "§8]§r ";
    }

    @Override
    public void debug(@NotNull String format, @NotNull Object... args) {
        debug(ChatColor.BLUE, format, args);
    }

    @Override
    public void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        log(color, "DEBUG", format, args);
    }

    @Override
    public void info(@NotNull String format, @NotNull Object... args) {
        info(ChatColor.GREEN, format, args);
    }

    @Override
    public void info(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        log(color, "INFO", format, args);
    }

    @Override
    public void warn(@NotNull String format, @NotNull Object... args) {
        warn(ChatColor.YELLOW, format, args);
    }

    @Override
    public void warn(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        log(color, "WARN", format, args);
    }

    @Override
    public void error(@NotNull String format, @NotNull Object... args) {
        error(ChatColor.RED, format, args);
    }

    @Override
    public void error(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        log(color, "ERROR", format, args);
    }

    @Override
    public void error(@NotNull String format, @NotNull Throwable throwable, @NotNull Object... args) {
        error(format + ": " + throwable.getClass().getSimpleName() + "\n" + stackTraceToString(throwable), args);
    }

    @Override
    public @NotNull CompletableFuture<Void> debugAsync(@NotNull String format, @NotNull Object... args) {
        // TODO
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> infoAsync(@NotNull String format, @NotNull Object... args) {
        // TODO
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> warnAsync(@NotNull String format, @NotNull Object... args) {
        // TODO
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> errorAsync(@NotNull String format, @NotNull Object... args) {
        // TODO
        return null;
    }

    @Override
    public void logToFile(@NotNull String level, @NotNull String format, @NotNull Object... args) {
        // TODO
    }

    @Override
    public void setLogLevel(@NotNull LogLevel level) {
        // TODO
    }

    @Override
    public @NotNull LogLevel getLogLevel() {
        // TODO
        return null;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    private int countPlaceholders(@NotNull String format) {
        int count = 0, index = 0;
        while ((index = format.indexOf("{}", index)) != -1) {
            count++;
            index += 2;
        }
        return count;
    }

    private String replacePlaceholders(@NotNull String format, @NotNull Object... args) {
        if (args.length == 0) return format;
        StringBuilder sb = new StringBuilder(format);
        int argIndex = 0, placeholderIndex;
        while ((placeholderIndex = sb.indexOf("{}")) != -1 && argIndex < args.length) {
            String argStr = args[argIndex].toString();
            sb.replace(placeholderIndex, placeholderIndex + 2, argStr);
            argIndex++;
        }
        return sb.toString();
    }

    private String stackTraceToString(@NotNull Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }
        return sw.toString();
    }

    private void log(@NotNull ChatColor defaultColor, @NotNull String level, @NotNull String format, @NotNull Object... args) {
        // 参数占位符数量检查
        int placeholders = countPlaceholders(format);
        if (args.length < placeholders) {
            throw new IllegalArgumentException("Expected " + placeholders + " arguments for format string, but got " + args.length);
        }

        String msg = replacePlaceholders(format, args);
        Bukkit.getLogger().info(getConsolePrefix() + "§8[" + defaultColor + "§l§n" + level + "§8]§r " + defaultColor + msg);
    }
}