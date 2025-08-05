package cn.yvmou.ylib.spigot.command;

import cn.yvmou.ylib.api.command.CommandManager;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.exception.YLibException;
import cn.yvmou.ylib.common.services.LoggerService;
import cn.yvmou.ylib.common.utils.ValidationUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spigot命令管理器实现
 *
 * @author yvmou
 * @since 1.0.0
 */
public class SpigotCommandManager implements CommandManager {
    
    private final Plugin plugin;
    private final LoggerService logger;
    private final Map<String, CommandExecutor> registeredCommands = new HashMap<>();
    private final Map<String, SubCommand[]> subCommands = new HashMap<>();
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public SpigotCommandManager(@NotNull Plugin plugin) {
        ValidationUtils.notNull(plugin, "Plugin");
        this.plugin = plugin;
        this.logger = new LoggerService(plugin);
    }
    
    @Override
    public void registerCommands(@NotNull String commandName, @NotNull SubCommand... subCommands) {
        ValidationUtils.notEmpty(commandName, "Command name");
        ValidationUtils.notNull(subCommands, "Sub commands");
        
        if (!ValidationUtils.isValidCommandName(commandName)) {
            throw new YLibException("Invalid command name: " + commandName);
        }
        
        this.subCommands.put(commandName, subCommands);
        
        // 注册主命令
        SpigotMainCommand mainCommand = new SpigotMainCommand(plugin, commandName, subCommands);
        plugin.getServer().getPluginCommand(commandName).setExecutor(mainCommand);
        plugin.getServer().getPluginCommand(commandName).setTabCompleter(mainCommand);
        
        // 创建适配器并存储
        CommandExecutor adapter = (sender, command, label, args) -> {
            // 这里我们直接返回true，因为实际的命令处理在SpigotMainCommand中完成
            return true;
        };
        registeredCommands.put(commandName, adapter);
        logger.command("Registered command: " + commandName + " with " + subCommands.length + " sub-commands");
    }
    
    @Override
    public void unregisterCommand(@NotNull String commandName) {
        ValidationUtils.notEmpty(commandName, "Command name");
        
        registeredCommands.remove(commandName);
        subCommands.remove(commandName);
        logger.command("Unregistered command: " + commandName);
    }
    
    @Override
    public boolean isCommandRegistered(@NotNull String commandName) {
        return registeredCommands.containsKey(commandName);
    }
    
    @Override
    public int getRegisteredCommandCount() {
        return registeredCommands.size();
    }
    
    @Override
    @NotNull
    public String[] getRegisteredCommands() {
        return registeredCommands.keySet().toArray(new String[0]);
    }
    
    /**
     * Spigot主命令实现
     */
    private static class SpigotMainCommand implements org.bukkit.command.CommandExecutor, TabCompleter {
        
        private final Plugin plugin;
        private final String commandName;
        private final SubCommand[] subCommands;
        
        public SpigotMainCommand(@NotNull Plugin plugin, @NotNull String commandName, @NotNull SubCommand[] subCommands) {
            this.plugin = plugin;
            this.commandName = commandName;
            this.subCommands = subCommands;
        }
        
        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                               @NotNull String label, @NotNull String[] args) {
            if (args.length == 0) {
                // 显示帮助信息
                sender.sendMessage("§6=== " + commandName + " 命令帮助 ===");
                for (SubCommand subCommand : subCommands) {
                    sender.sendMessage("§e/" + commandName + " " + subCommand.getName() + 
                                    " §7- " + subCommand.getDescription());
                }
                return true;
            }
            
            String subCommandName = args[0].toLowerCase();
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().equalsIgnoreCase(subCommandName) || 
                    hasAlias(subCommand, subCommandName)) {
                    
                    // 检查权限
                    if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                        sender.sendMessage("§c你没有权限使用此命令！");
                        return true;
                    }
                    
                    // 检查是否只允许玩家执行
                    if (subCommand.isOnlyPlayer() && !(sender instanceof org.bukkit.entity.Player)) {
                        sender.sendMessage("§c此命令只能由玩家执行！");
                        return true;
                    }
                    
                    // 创建新的参数数组，移除子命令名称
                    String[] subArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                    
                    // 执行子命令
                    return subCommand.execute(sender, subArgs);
                }
            }
            
            sender.sendMessage("§c未知的子命令: " + subCommandName);
            return true;
        }
        
        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                        @NotNull String alias, @NotNull String[] args) {
            List<String> completions = new ArrayList<>();
            
            if (args.length == 1) {
                // 补全子命令名称
                for (SubCommand subCommand : subCommands) {
                    if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                        completions.add(subCommand.getName());
                        String[] aliases = subCommand.getAliases();
                        if (aliases != null && aliases.length > 0) {
                            for (String aliasName : aliases) {
                                completions.add(aliasName);
                            }
                        }
                    }
                }
            } else if (args.length > 1) {
                // 查找子命令并调用其tab补全
                String subCommandName = args[0].toLowerCase();
                for (SubCommand subCommand : subCommands) {
                    if (subCommand.getName().equalsIgnoreCase(subCommandName) || 
                        hasAlias(subCommand, subCommandName)) {
                        
                        if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission())) {
                            // 创建新的参数数组，移除子命令名称
                            String[] subArgs = new String[args.length - 1];
                            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                            
                            // 调用子命令的tab补全
                            return subCommand.tabComplete(sender, subArgs);
                        }
                        break;
                    }
                }
            }
            
            return completions;
        }
        
        private boolean hasAlias(@NotNull SubCommand subCommand, @NotNull String alias) {
            String[] aliases = subCommand.getAliases();
            if (aliases != null && aliases.length > 0) {
                for (String aliasName : aliases) {
                    if (aliasName.equalsIgnoreCase(alias)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}