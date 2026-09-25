package cn.yvmou.ylib.logger;

import cn.yvmou.ylib.PluginInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MessageImpl implements Message {
    private final CommandSender target;

    protected MessageImpl(CommandSender target) {
        this.target = target;
    }

    @Override
    public void msg(@NotNull String format, @NotNull Object... args) {
        String msg = LoggerUtil.formatMessage(format, args);
        target.sendMessage(msg);
    }
}
