package cn.yvmou.ylib.impl.command.core;

import cn.yvmou.ylib.api.command.exception.CommandException;
import cn.yvmou.ylib.api.command.tree.CommandNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 适配新命令系统的 YLibCommand
 */
public class YLibCommandAdapter extends Command {
    private final CommandNode root;
    private final CommandDispatcher dispatcher;
    private BiConsumer<CommandSender, CommandException> exceptionHandler;

    public YLibCommandAdapter(String name, Plugin plugin, CommandNode root, CommandDispatcher dispatcher) {
        super(name);
        this.root = root;
        this.dispatcher = dispatcher;
        
        // 设置基本信息
        if (root.getDescription() != null) {
            setDescription(root.getDescription());
        }
        if (root.getPermission() != null) {
            setPermission(root.getPermission());
        }
    }
    
    public void setExceptionHandler(BiConsumer<CommandSender, CommandException> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            dispatcher.execute(root, sender, args, commandLabel);
            return true;
        } catch (CommandException e) {
            if (exceptionHandler != null) {
                exceptionHandler.accept(sender, e);
            } else {
                sender.sendMessage("§c" + e.getMessage());
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c命令执行时发生内部错误");
            e.printStackTrace();
            return false;
        }
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        try {
            return dispatcher.tabComplete(root, sender, args);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
