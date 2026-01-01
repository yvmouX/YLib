package cn.yvmou.ylib.api.command;

import org.jetbrains.annotations.NotNull;

/**
 * 命令管理器接口
 * <p>
 * 提供简化的命令注册和管理功能，支持主命令和子命令的统一管理。
 * 通过注解配置命令选项，自动处理权限检查、参数验证、别名注册等。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 注册命令
 * commandManager.registerCommands("myplugin",
 *     new ReloadCommand(),
 *     new InfoCommand(),
 *     new AdminCommand()
 * );
 *
 * // 子命令示例
 * public class ReloadCommand implements SubCommand {
 *     @CommandOptions(
 *         name = "reload",
 *         permission = "myplugin.admin.reload",
 *         onlyPlayer = false,
 *         alias = {"rl", "restart"},
 *         usage = "/myplugin reload"
 *     )
 *     public boolean execute(CommandSender sender, String[] args) {
 *         // 重载逻辑
 *         sender.sendMessage("§a配置已重载！");
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface CommandManager {

    /**
     * 注册主命令及其子命令
     * <p>
     * 该方法会自动扫描子命令类中的 {@link CommandOptions} 注解，
     * 并根据配置注册相应的命令和别名。支持以下功能：
     * </p>
     * <ul>
     *   <li>自动权限检查</li>
     *   <li>玩家限制检查</li>
     *   <li>命令别名注册</li>
     *   <li>使用说明显示</li>
     *   <li>配置文件管理</li>
     * </ul>
     *
     * @param commandName 主命令名称
     * @param subCommands 子命令实例数组
     */
    void registerCommands(@NotNull String commandName, @NotNull SubCommand... subCommands);
}