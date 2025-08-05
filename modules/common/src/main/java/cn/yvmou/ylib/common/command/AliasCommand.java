package cn.yvmou.ylib.common.command;

import cn.yvmou.ylib.api.services.LoggerService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 别名命令执行器
 * <p>
 * 处理命令别名的执行，将别名命令重定向到主命令。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class AliasCommand implements CommandExecutor {
    
    private final Plugin plugin;
    private final String mainCommandName;
    private final String subCommandName;
    private final LoggerService logger;
    
    /**
     * 构造函数
     * 
     * @param plugin 插件实例
     * @param mainCommandName 主命令名称
     * @param subCommandName 子命令名称
     * @param logger 日志服务
     */
    public AliasCommand(@NotNull Plugin plugin, @NotNull String mainCommandName, 
                       @NotNull String subCommandName, @NotNull LoggerService logger) {
        this.plugin = plugin;
        this.mainCommandName = mainCommandName;
        this.subCommandName = subCommandName;
        this.logger = logger;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        // 构造新的命令字符串，将别名转换为主命令+子命令的形式
        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append(mainCommandName).append(" ").append(subCommandName);
        
        // 添加原始参数
        for (String arg : args) {
            commandBuilder.append(" ").append(arg);
        }
        
        String fullCommand = commandBuilder.toString();
        
        logger.debug("别名命令重定向: /" + label + " -> /" + fullCommand);
        
        // 执行重定向的命令
        try {
            return Bukkit.dispatchCommand(sender, fullCommand);
        } catch (Exception e) {
            logger.error("执行别名命令重定向时发生错误: " + e.getMessage(), e);
            return false;
        }
    }
}