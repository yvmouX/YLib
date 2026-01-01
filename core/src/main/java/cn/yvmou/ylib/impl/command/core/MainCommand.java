package cn.yvmou.ylib.impl.command.core;

import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.logger.Logger;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.api.services.ServerInfoService;
import cn.yvmou.ylib.impl.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 主命令执行器
 * <p>
 * 处理主命令的执行逻辑，包括子命令分发、权限检查、参数验证等。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public class MainCommand implements CommandExecutor {
    private final String mainCommandName;
    private Map<String, SubCommand> requireRegisterConfigSubCommandClassMap = new HashMap<>();
    private final Logger logger;
    private final MessageService message;
    private final ServerInfoService serverInfo;
    private final CommandConfig commandConfig;

    public MainCommand(Logger logger, MessageService message, String mainCommandName, Map<String, SubCommand> requireRegisterConfigSubCommandClassMap, ServerInfoService serverInfo, CommandConfig commandConfig) {
        this.mainCommandName = mainCommandName;
        this.logger = logger;
        this.message = message;
        this.requireRegisterConfigSubCommandClassMap = requireRegisterConfigSubCommandClassMap;
        this.serverInfo = serverInfo;
        this.commandConfig = commandConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 如果没有参数，显示版本信息。
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(serverInfo.getPluginPrefix(), "version " + serverInfo.getPluginVersion()));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = requireRegisterConfigSubCommandClassMap.get(subCommandName);

        // 如果子命令不存在 显示可用命令列表
        if (subCommand == null) {
            message.sendMessage(sender, "&cUnknown subcommand '" + subCommandName + "'");
            message.sendMessage(sender, "&cAvailable subCommands：");
            if (requireRegisterConfigSubCommandClassMap.isEmpty()) {
                message.sendMessage(sender, "&cNo subcommand found");
            } else {
                message.sendMessage(sender, requireRegisterConfigSubCommandClassMap.keySet().toString());
            }
            return true;
        }

        // 检查是否只允许玩家执行
        if (commandConfig.isPlayerOnly(mainCommandName, subCommandName) && !(sender instanceof org.bukkit.entity.Player)) {
            message.sendMessage(sender, "&c" + subCommandName + " command can only be executed by player");
            return true;
        }

        // 检查权限
        String permission = commandConfig.getCommandPermission(mainCommandName, subCommandName);
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            message.sendMessage(sender, "&c" + mainCommandName + " You do not have permission to use this command");
            return true;
        }

        return subCommand.execute(sender, args);
    }
}

