package cn.yvmou.ylib.logger;

import cn.yvmou.ylib.PluginInfo;
import cn.yvmou.ylib.api.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class LoggerImpl implements Logger {
    private CommandSender target;

    public LoggerImpl() {
    }

    // 私有构造，用于创建临时的 Logger 实例
    private LoggerImpl(CommandSender target) {
        this.target = target;
    }

    @Override
    public void debug(@NotNull String format, @NotNull Object... args) {
        debug(ChatColor.BLUE, format, args);
    }

    @Override
    public void debug(@NotNull ChatColor color, @NotNull String format, @NotNull Object... args) {
        if (!PluginInfo.getLoggerDebug()) return;
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
    public Logger to(@NotNull CommandSender sender) {
        // 返回一个新的不可变实例，保证线程安全
        return new LoggerImpl(sender);
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    private void log(@NotNull ChatColor levelColor, @NotNull String levelName, @NotNull String format, @NotNull Object... args) {
        String msg = formatMessage(format, args);
        String fullMessage = String.format("%s§8[%s§l§n%s§8]§r %s%s",
            PluginInfo.getLoggerPrefix(), levelColor, levelName, levelColor, msg);

        if (target != null) {
            target.sendMessage(fullMessage);
        } else {
            // 默认发送到控制台
            Bukkit.getConsoleSender().sendMessage(fullMessage);
        }
    }

    /**
     * 格式化消息，支持 {} 占位符
     */
    private String formatMessage(@NotNull String format, @NotNull Object... args) {
        if (args == null || args.length == 0) {
            return format;
        }
        
        // 简单的 {} 替换实现，比正则更高效
        StringBuilder sb = new StringBuilder(format.length() + 50);
        int argIndex = 0;
        int lastIndex = 0;
        
        while (argIndex < args.length) {
            int placeholderIndex = format.indexOf("{}", lastIndex);
            if (placeholderIndex == -1) {
                break;
            }
            
            sb.append(format, lastIndex, placeholderIndex);
            sb.append(args[argIndex++]);
            lastIndex = placeholderIndex + 2;
        }
        
        sb.append(format.substring(lastIndex));
        return sb.toString();
    }
}
