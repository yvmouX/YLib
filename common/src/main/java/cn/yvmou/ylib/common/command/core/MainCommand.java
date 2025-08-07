package cn.yvmou.ylib.common.command.core;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.services.MessageService;
import cn.yvmou.ylib.common.command.CommandConfig;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Plugin plugin;
    private final String commandName;
    private final Map<String, SubCommand> subCommands = new HashMap<>(); // 子命令 SubCommand
    private final Map<String, CommandOptions> commandOptions = new HashMap<>();
    private CommandConfig commandConfig;
    private final LoggerService logger;
    private final MessageService message;

    public MainCommand(Plugin plugin, LoggerService logger, MessageService message, String commandName, SubCommand... subCommands) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.logger = logger;
        this.message = message;
        commandConfig = new CommandConfig(plugin, logger);

        // 初始化配置
        commandConfig.initCommandConfig(commandName, subCommands);
        // 注册子子命令
        registerSubCommands(commandName, subCommands);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 如果没有参数，显示版本信息。
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "version " + ylib.getServerInfo().getPluginVersion()));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "&cunknown command:&f " + subCommandName + "\n" +
                    "&a可用子命令列表："));
            List<String> s = commandConfig.getSubCommandList(command.getName());
            for (String cmd : s) {
                message.sendMessage(sender, cmd);
            }
            return true;
        }

        CommandOptions options = commandOptions.get(subCommandName);
        if (options == null) {
            logger.warn("Can't find command options for subcommand " + subCommandName);
            return false;
        }

        // 检查命令是否启用
        if (!commandConfig.isCommandEnabled(commandName, subCommandName)) {
            message.sendMessage(sender, "&c" + commandName + " is not enabled");
            return true;
        }

        // 检查是否只运行玩家执行
        if (commandConfig.isPlayerOnly(commandName, subCommandName)) {
            message.sendMessage(sender, "&c" + commandName + " can only be used with players ");
            return true;
        }

        // 检查权限
        String permission = commandConfig.getCommandPermission(commandName, subCommandName);
        if (!permission.isEmpty() && sender.hasPermission(permission)) {
            message.sendMessage(sender, "&c" + commandName + " You do not have permission to use this command");
            return true;
        }

        return subCommand.execute(sender, args);
    }

    private void registerSubCommands(String commandName, SubCommand... subCommands) {
        // 获取到所有的子命令
        try {
            for (SubCommand subCommand : subCommands) {
                Method method = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);

                if (method.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions commandOptions = method.getAnnotation(CommandOptions.class);

                    this.subCommands.put(commandOptions.name().toLowerCase(), subCommand);
                }
            }
        } catch (NoSuchMethodException ignored) {
        }

        // 移除未注册命令
        ConfigurationSection commands = commandConfig.getConfig().getConfigurationSection("commands." + commandName);
        if (commands != null) {
            commands.getKeys(false).forEach(cmd -> {
                boolean b = commandConfig.getConfig().getBoolean("commands." + commandName + "." + cmd + ".register", true);
                if (!b) {
                    this.subCommands.remove(cmd);
                    logger.warn("已移除未注册命令: " + cmd);
                }
            });
        }
    }
}

