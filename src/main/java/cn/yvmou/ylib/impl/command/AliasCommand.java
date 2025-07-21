package cn.yvmou.ylib.impl.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class AliasCommand implements CommandExecutor {
    private final Plugin plugin;
    private final String commandName;
    private final String originalCommand;

    public AliasCommand(Plugin plugin, String commandName, String originalCommand) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.originalCommand = originalCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = originalCommand;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return plugin.getServer().dispatchCommand(sender, commandName+ " " + String.join(" ", newArgs));
    }
}
