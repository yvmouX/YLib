package cn.yvmou.ylib.impl.command.wrapped;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.api.command.tree.CommandNode;
import cn.yvmou.ylib.impl.command.CommandDispatcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class WrappedCommand extends Command {
    private final CommandNode rootNode;
    private final CommandDispatcher dispatcher;

    public WrappedCommand(@NotNull String name, CommandNode rootNode, CommandDispatcher dispatcher) {
        super(name);
        this.rootNode = rootNode;
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            dispatcher.execute(rootNode, sender, args, commandLabel);
            return true;
        } catch (Exception e ) {
            YLib._getLogger().error("Error executing command: " + e.getMessage(), e);
            return false;
        }
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        try {
            return dispatcher.tabComplete(rootNode, sender, args);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
