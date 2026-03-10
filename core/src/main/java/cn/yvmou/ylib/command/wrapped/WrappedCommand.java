package cn.yvmou.ylib.command.wrapped;

import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.command.CommandDispatcher;
import cn.yvmou.ylib.command.exception.CommandParseException;
import cn.yvmou.ylib.command.exception.CommandValidationException;
import cn.yvmou.ylib.command.tree.CommandNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class WrappedCommand extends Command {
    private final CommandNode rootNode;
    private final CommandDispatcher dispatcher;
    private final Logger logger;

    public WrappedCommand(@NotNull String name, CommandNode rootNode, CommandDispatcher dispatcher, Logger logger) {
        super(name);
        this.rootNode = rootNode;
        this.dispatcher = dispatcher;
        this.logger = logger;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        try {
            dispatcher.execute(rootNode, sender, args, commandLabel);
            return true;
        } catch (CommandParseException | CommandValidationException e) {
            // 预期内的命令错误（参数错误、验证失败等），直接发给玩家，不记录堆栈
            logger.to(sender).error(e.getMessage());
            return true;
        } catch (Exception e) {
            // 预期外的异常（NPE、数据库错误等），记录堆栈并通知玩家
            logger.error("Error executing command: " + e.getMessage(), e);
            logger.to(sender).error("命令执行期间发生内部错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return true; 
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
