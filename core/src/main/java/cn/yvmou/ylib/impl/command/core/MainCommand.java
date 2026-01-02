package cn.yvmou.ylib.impl.command.core;

import cn.yvmou.ylib.PluginInfo;
import cn.yvmou.ylib.api.command.CommandConfig;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.logger.Logger;
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
    private final CommandConfig commandConfig;

    public MainCommand(Logger logger, String mainCommandName, Map<String, SubCommand> requireRegisterConfigSubCommandClassMap, CommandConfig commandConfig) {
        this.mainCommandName = mainCommandName;
        this.logger = logger;
        this.requireRegisterConfigSubCommandClassMap = requireRegisterConfigSubCommandClassMap;
        this.commandConfig = commandConfig;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 如果没有参数，显示版本信息。
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(PluginInfo.getPluginPrefix(), "version " + PluginInfo.getPluginVersion()));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = requireRegisterConfigSubCommandClassMap.get(subCommandName);

        // 如果子命令不存在 显示可用命令列表
        if (subCommand == null) {
            logger.toPlayer(sender).info("&cUnknown subcommand '" + subCommandName + "'");
            logger.toPlayer(sender).info("&cAvailable subCommands：");
            if (requireRegisterConfigSubCommandClassMap.isEmpty()) {
                logger.toPlayer(sender).info("&cNo subcommand found");
            } else {
                logger.toPlayer(sender).info(requireRegisterConfigSubCommandClassMap.keySet().toString());
            }
            return true;
        }

        // 检查是否只允许玩家执行
        if (commandConfig.isPlayerOnly(mainCommandName, subCommandName) && !(sender instanceof org.bukkit.entity.Player)) {
            logger.toPlayer(sender).warn("&c" + subCommandName + " command can only be executed by player");
            return true;
        }

        // 检查权限
        String permission = commandConfig.getCommandPermission(mainCommandName, subCommandName);
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            logger.toPlayer(sender).warn("&c" + subCommandName + " command permission denied");
            return true;
        }

        return subCommand.execute(sender, args);
    }
}

