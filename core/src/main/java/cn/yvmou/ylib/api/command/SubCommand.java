package cn.yvmou.ylib.api.command;

import org.bukkit.command.CommandSender;

/**
 * 子命令接口
 * <p>
 * 用于定义子命令的执行逻辑。子命令可以通过 {@link CommandOptions} 注解
 * 进行配置，包括权限、使用限制、别名等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface SubCommand {
    
    /**
     * 执行子命令
     * 
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 如果命令执行成功返回true，否则返回false
     */
    boolean execute(CommandSender sender, String[] args);
}