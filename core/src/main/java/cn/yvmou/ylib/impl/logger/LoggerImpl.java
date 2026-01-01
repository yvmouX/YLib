package cn.yvmou.ylib.impl.logger;

import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.enums.LoggerOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class LoggerImpl implements Logger {
    private final String prefix;
    private final Boolean debug;
    private final Boolean ansi;

    public LoggerImpl(String prefix) {
        this.prefix = prefix;
        ansi = false;
        debug = true;
    }

    public LoggerImpl(String prefix, LoggerOption option) {
        this.prefix = prefix;
        debug = option == LoggerOption.DEBUG;
        ansi = option == LoggerOption.ANSI;
    }

    @Override
    public void debug(@NotNull String format, @NotNull Object... args) {
        debug(ChatColor.BLUE, format, args);
    }

    @Override
    public void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        if (!debug) return;

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


    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    private void log(@NotNull ChatColor defaultColor, @NotNull String level, @NotNull String format, @NotNull Object... args) {
        String msg = replacePlaceholders(format, args);
        if (ansi) {
            String ansiColor = mapChatColorToAnsi(defaultColor);
            String ansiOutput = ansiColor + "[" + level + "] " + msg + ANSI_RESET;
            System.out.println(ansiOutput);
        } else {
            String bukkitOutput = prefix + "§8[" + defaultColor + "§l§n" + level + "§8]§r " + defaultColor + msg;
            Bukkit.getConsoleSender().sendMessage(bukkitOutput);
        }
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

    // ANSI 颜色码映射
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private static String mapChatColorToAnsi(ChatColor color) {
        switch (color) {
            case BLACK: return ANSI_BLACK;
            case DARK_RED: return ANSI_RED;
            case DARK_GREEN: return ANSI_GREEN;
            case GOLD: return ANSI_YELLOW;
            case DARK_BLUE: return ANSI_BLUE;
            case DARK_PURPLE: return ANSI_PURPLE;
            case AQUA: return ANSI_CYAN;
            case WHITE: return ANSI_WHITE;
            case RED: return ANSI_RED;
            case GREEN: return ANSI_GREEN;
            case YELLOW: return ANSI_YELLOW;
            case BLUE: return ANSI_BLUE;
            case LIGHT_PURPLE: return ANSI_PURPLE;
            case DARK_AQUA: return ANSI_CYAN;
            case GRAY: return ANSI_WHITE;
            default: return ANSI_RESET;
        }
    }
}
