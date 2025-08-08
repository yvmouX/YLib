package cn.yvmou.ylib.common.command.core;

import cn.yvmou.ylib.api.services.LoggerService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 别名命令执行器
 * <p>
 * 处理命令别名的执行，将别名命令重定向到主命令。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class AliasCommand implements CommandExecutor {
    
    private final Plugin plugin;
    private final String mainCommandName;
    private final String originalCommand;

    public AliasCommand(@NotNull Plugin plugin, @NotNull String mainCommandName, @NotNull String originalCommand) {
        this.plugin = plugin;
        this.mainCommandName = mainCommandName;
        this.originalCommand = originalCommand;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = originalCommand;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return plugin.getServer().dispatchCommand(sender, mainCommandName+ " " + String.join(" ", newArgs));
    }
}