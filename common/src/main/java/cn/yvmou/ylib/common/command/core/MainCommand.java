package cn.yvmou.ylib.common.command.core;

import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static cn.yvmou.ylib.common.YLibImpl.ylib;

/**
 * 主命令执行器
 * <p>
 * 处理主命令的执行逻辑，包括子命令分发、权限检查、参数验证等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class MainCommand implements CommandExecutor {
    private final String mainCommandName;
    private Map<String, SubCommand> subCommandClassList = new HashMap<>();
   // private final Map<String, SubCommand> newSubCommandClassList = new HashMap<>(); // 存储已注册的子命令，键为子命令名称，值为子命令实例
    private final LoggerService logger;
    private final MessageService message;

    public MainCommand(LoggerService logger, MessageService message, String mainCommandName, Map<String, SubCommand> subCommandClassList) {
        this.mainCommandName = mainCommandName;
        this.logger = logger;
        this.message = message;

        this.subCommandClassList = subCommandClassList;
//        // 注册 需要执行的 子命令
//        requireExecuteSubCommands(subCommandClassList);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 如果没有参数，显示版本信息。
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "version " + ylib.getServerInfo().getPluginVersion()));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommandClassList.get(subCommandName);

        // 如果子命令不存在 显示可用命令列表
        if (subCommand == null) {
            message.sendMessage(sender, "&cUnknown subcommand '" + subCommandName + "'");
            message.sendMessage(sender, "&a可用子命令列表：");
            if (subCommandClassList.isEmpty()) {
                message.sendMessage(sender, "&cNo subcommand found");
            } else {
                message.sendMessage(sender, subCommandClassList.keySet().toString());
            }
            return true;
        }

        // 检查命令是否启用
        if (!ylib.getSimpleCommandManager().getCommandConfig().isCommandRegister(mainCommandName, subCommandName)) {
            message.sendMessage(sender, "&c" + mainCommandName + " is not registered");
            return true;
        }

        // 检查是否只允许玩家执行
        if (ylib.getSimpleCommandManager().getCommandConfig().isPlayerOnly(mainCommandName, subCommandName) && !(sender instanceof org.bukkit.entity.Player)) {
            message.sendMessage(sender, "&c" + subCommandName + " 命令只能由玩家执行");
            return true;
        }

        // 检查权限
        String permission = ylib.getSimpleCommandManager().getCommandConfig().getCommandPermission(mainCommandName, subCommandName);
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            message.sendMessage(sender, "&c" + mainCommandName + " You do not have permission to use this command");
            return true;
        }

        return subCommand.execute(sender, args);
    }

//    private void requireExecuteSubCommands(Map<String, SubCommand> subCommandClassList) {
//        // 获取到所有的子命令
//        for (Map.Entry<String, SubCommand> entry : subCommandClassList.entrySet()) {
//            String subCommandName = entry.getKey();
//            SubCommand subCommand = entry.getValue();
//            subCommandClassList.put(subCommandName.toLowerCase(), subCommand);
//            logger.info("注册子命令: " + subCommandName);
//        }
//
//        // 移除未注册命令
//        List<String> commands = ylib.getSimpleCommandManager().getCommandConfig().getSubCommandList(mainCommandName);
//        for (String command : commands) {
//            boolean isRegister = ylib.getSimpleCommandManager().getCommandConfig().isCommandRegister(mainCommandName, command);
//            if (!isRegister) {
//                newSubCommandClassList.remove(command.toLowerCase());
//                logger.warn("已移除未注册命令: " + command);
//            }
//        }
//    }
}

