package cn.yvmou.ylib.api.logger;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public interface Logger {
    void debug(@NotNull String format, @NotNull Object... args);

    void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    void info(@NotNull String format, @NotNull Object... args);

    void info(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    void warn(@NotNull String format, @NotNull Object... args);

    void warn(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);

    void error(@NotNull String format, @NotNull Object... args);

    void error(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args);
}
