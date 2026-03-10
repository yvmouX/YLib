package cn.yvmou.ylib.api.command;

import cn.yvmou.ylib.command.annotation.Command;
import cn.yvmou.ylib.command.annotation.SubCommand;
import org.jetbrains.annotations.NotNull;

/**
 * 命令管理器接口
 * <p>
 * 提供命令注册和管理功能，支持基于注解的命令系统。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Command(name = "mycmd", description = "My Command")
 * public class MyCommand {
 *     @SubCommand("test")
 *     public void test(CommandSender sender) {
 *         // ...
 *     }
 * }
 * 
 * // 注册命令
 * commandManager.register(new MyCommand());
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0
 */
public interface CommandManager {

    /**
     * 注册命令
     * <p>
     * 该方法会自动扫描类中的 {@link Command} 和
     * {@link SubCommand} 注解，
     * 并注册相应的命令。
     * </p>
     *
     * @param commandInstance 带有注解的命令实例
     */
    void register(@NotNull Object commandInstance);
    
    /**
     * 重新加载所有命令配置
     * <p>
     * 重新读取 commands.yml 并将配置应用到所有已注册的命令上。
     * 支持更新权限、描述、别名和启用状态。
     * </p>
     */
    void reload();
}
