package cn.yvmou.ylib.api.command;

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
     * 该方法会自动扫描类中的 {@link cn.yvmou.ylib.api.command.annotation.Command} 和 
     * {@link cn.yvmou.ylib.api.command.annotation.SubCommand} 注解，
     * 并注册相应的命令。
     * </p>
     *
     * @param commandInstance 带有注解的命令实例
     */
    void register(@NotNull Object commandInstance);
}
