package cn.yvmou.ylib.impl.command;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.api.command.*;
import cn.yvmou.ylib.tools.LoggerTools;
import cn.yvmou.ylib.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor {
    private final Plugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>(); // 子命令 SubCommand
    private CommandConfig commandOptionsConfig;
    private final LoggerTools logger = YLib.getyLib().getLoggerTools();

    public MainCommand(Plugin plugin, String commandName, SubCommand... commandHolders) {
        this.plugin = plugin;
        initCommandConfig(commandName, commandHolders);
        init(commandName, commandHolders);
    }

    private void initCommandConfig(String commandName, SubCommand... commandHolders) {
        commandOptionsConfig = new CommandConfig(plugin);
        commandOptionsConfig.initCommandConfig(commandName, commandHolders);
    }

    private void init(String commandName, SubCommand... commandHolders) {
        // 获取到所有的子命令
        try {
            for (SubCommand subCommand : commandHolders) {
                Method method = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);
                if (method.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions commandOptions = method.getAnnotation(CommandOptions.class);

                    subCommands.put(commandOptions.name(), subCommand);
                }
            }
        } catch (NoSuchMethodException ignored) {}

        // 迭代器
        // 移除未注册命令
        ConfigurationSection commands = commandOptionsConfig.getConfig().getConfigurationSection("commands." + commandName);
        if (commands != null) {
            commands.getKeys(false).forEach(cmd -> {
                boolean b = commandOptionsConfig.getConfig().getBoolean("commands." + commandName + "." + cmd + ".register", true);
                if (!b) {
                    subCommands.remove(cmd);
                    logger.warn("已移除未注册命令: " + cmd);
                }
            });
        }
//        Iterator<Map.Entry<String, SubCommand>> it = subCommands.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<String, SubCommand> entry = it.next();
//            if (!entry.getValue().requireRegister()) {
//                it.remove();
//                logger.warn("已移除未注册命令: " + entry.getKey());
//            }
//        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "version " + YLib.getyLib().getVersion()));
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0]);

        if (subCommand == null) {
            sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "&c用法错误！可用子命令："));

            List<String> s = commandOptionsConfig.getSubCommandList(command.getName());

            for (String cmd : s) {
                sender.sendMessage(MessageUtils.oldColorWithPrefix(null, cmd));
            }

            return true;
        }

        // 注解设置
        try {
            Method method = subCommand.getClass().getDeclaredMethod("execute", CommandSender.class, String[].class);

            if (method.isAnnotationPresent(CommandOptions.class)) {
                CommandOptions commandOptions = method.getAnnotation(CommandOptions.class);

                // 权限
                if (!commandOptions.permission().isEmpty() && !sender.hasPermission(commandOptions.permission())) {
                    sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "&c你没有权限执行这个命令。&7(" + commandOptions.permission() + ")"));
                    return true;
                }

                // 是否仅玩家使用
                if (commandOptions.onlyPlayer() && !(sender instanceof Player)) {
                    sender.sendMessage(MessageUtils.oldColorWithPrefix(null, "&c该命令仅玩家可用"));
                    return true;
                }
            }
        } catch (NoSuchMethodException ignored) {
        }

        return subCommand.execute(sender, args);
    }
}
