package cn.yvmou.ylib.command.tree;

import cn.yvmou.ylib.command.context.CommandContext;
import org.bukkit.command.CommandSender;

/**
 * 命令执行器接口
 */
@FunctionalInterface
public interface CommandExecutor {
    /**
     * 执行命令
     * @param sender 命令发送者
     * @param context 命令上下文
     */
    void execute(CommandSender sender, CommandContext context) throws Exception;
}
