package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.command.CommandOptions;
import cn.yvmou.ylib.api.command.SubCommand;
import cn.yvmou.ylib.api.services.LoggerService;
import cn.yvmou.ylib.api.services.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final Map<String, CommandOptions> commandOptions = new HashMap<>();
    private final CommandConfig commandConfig;
    private final LoggerService logger;
    private final MessageService messageService;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param commandName 主命令名称
     * @param logger 日志服务
     * @param messageService 消息服务
     * @param subCommands 子命令数组
     */
    public MainCommand(@NotNull Plugin plugin, @NotNull String commandName, 
                      @NotNull LoggerService logger, @NotNull MessageService messageService,
                      @NotNull SubCommand... subCommands) {
        this.plugin = plugin;
        this.commandName = commandName;
        this.logger = logger;
        this.messageService = messageService;
        this.commandConfig = new CommandConfig(plugin, logger);
        
        // 初始化配置
        commandConfig.initCommandConfig(commandName, subCommands);
        
        // 注册子命令
        registerSubCommands(subCommands);
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        // 如果没有参数，显示帮助信息
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        
        // 查找子命令
        SubCommand subCommand = subCommands.get(subCommandName);
        if (subCommand == null) {
            messageService.sendMessage(sender, "&c未知命令: &e" + subCommandName);
            messageService.sendMessage(sender, "&7使用 &f/" + commandName + " help &7查看可用命令");
            return true;
        }
        
        CommandOptions options = commandOptions.get(subCommandName);
        if (options == null) {
            logger.warning("找不到命令 " + subCommandName + " 的配置选项");
            return false;
        }
        
        // 检查命令是否启用
        if (!commandConfig.isCommandEnabled(commandName, subCommandName)) {
            messageService.sendMessage(sender, "&c该命令已被禁用");
            return true;
        }
        
        // 检查是否只允许玩家执行
        if (commandConfig.isPlayerOnly(commandName, subCommandName) && !(sender instanceof Player)) {
            messageService.sendMessage(sender, "&c该命令只能由玩家执行");
            return true;
        }
        
        // 检查权限
        String permission = commandConfig.getCommandPermission(commandName, subCommandName);
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            messageService.sendMessage(sender, "&c你没有权限执行该命令");
            return true;
        }
        
        // 准备子命令参数（移除第一个参数，即子命令名称）
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        
        // 执行子命令
        try {
            boolean result = subCommand.execute(sender, subArgs);
            if (!result) {
                // 如果命令执行失败，显示使用方法
                String usage = commandConfig.getCommandUsage(commandName, subCommandName);
                messageService.sendMessage(sender, "&c命令使用方法: &f" + usage);
            }
            return result;
        } catch (Exception e) {
            logger.error("执行命令 " + subCommandName + " 时发生错误: " + e.getMessage(), e);
            messageService.sendMessage(sender, "&c命令执行时发生错误，请查看控制台日志");
            return false;
        }
    }
    
    /**
     * 注册子命令
     * 
     * @param subCommands 子命令数组
     */
    private void registerSubCommands(@NotNull SubCommand... subCommands) {
        for (SubCommand subCommand : subCommands) {
            try {
                Method executeMethod = subCommand.getClass().getDeclaredMethod("execute", 
                    CommandSender.class, String[].class);
                
                if (executeMethod.isAnnotationPresent(CommandOptions.class)) {
                    CommandOptions options = executeMethod.getAnnotation(CommandOptions.class);
                    String cmdName = options.name().toLowerCase();
                    
                    this.subCommands.put(cmdName, subCommand);
                    this.commandOptions.put(cmdName, options);
                    
                    logger.debug("注册子命令: " + cmdName);
                }
            } catch (NoSuchMethodException e) {
                logger.warning("子命令类 " + subCommand.getClass().getSimpleName() + 
                             " 没有正确的execute方法: " + e.getMessage());
            }
        }
    }
    
    /**
     * 显示帮助信息
     * 
     * @param sender 命令发送者
     */
    private void showHelp(@NotNull CommandSender sender) {
        messageService.sendMessage(sender, "&6=== " + plugin.getName() + " 命令帮助 ===");
        
        if (subCommands.isEmpty()) {
            messageService.sendMessage(sender, "&c没有可用的命令");
            return;
        }
        
        for (Map.Entry<String, CommandOptions> entry : commandOptions.entrySet()) {
            String cmdName = entry.getKey();
            CommandOptions options = entry.getValue();
            
            // 检查命令是否启用
            if (!commandConfig.isCommandEnabled(commandName, cmdName)) {
                continue;
            }
            
            // 检查权限
            String permission = commandConfig.getCommandPermission(commandName, cmdName);
            if (!permission.isEmpty() && !sender.hasPermission(permission)) {
                continue;
            }
            
            String usage = commandConfig.getCommandUsage(commandName, cmdName);
            messageService.sendMessage(sender, "&f" + usage + " &7- " + getCommandDescription(options));
        }
    }
    
    /**
     * 获取命令描述
     * 
     * @param options 命令选项
     * @return 命令描述
     */
    @NotNull
    private String getCommandDescription(@NotNull CommandOptions options) {
        // 这里可以根据需要添加更多的描述逻辑
        // 暂时返回基本信息
        StringBuilder desc = new StringBuilder();
        
        if (!options.permission().isEmpty()) {
            desc.append("需要权限: ").append(options.permission());
        }
        
        if (options.onlyPlayer()) {
            if (desc.length() > 0) desc.append(", ");
            desc.append("仅限玩家");
        }
        
        if (desc.length() == 0) {
            desc.append("执行 ").append(options.name()).append(" 命令");
        }
        
        return desc.toString();
    }
    
    /**
     * 获取命令配置
     * 
     * @return 命令配置实例
     */
    @NotNull
    public CommandConfig getCommandConfig() {
        return commandConfig;
    }
    
    /**
     * 获取已注册的子命令
     * 
     * @return 子命令名称集合
     */
    @NotNull
    public Set<String> getRegisteredSubCommands() {
        return subCommands.keySet();
    }
}