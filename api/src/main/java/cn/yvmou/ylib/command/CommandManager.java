package cn.yvmou.ylib.command;

import cn.yvmou.ylib.command.annotation.Command;
import cn.yvmou.ylib.command.annotation.SubCommand;
import cn.yvmou.ylib.command.tree.CommandNode;
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
     * 注册一棵直接建好的命令树。
     * <p>
     * 注解不够用时（自定义解析、复杂树结构）用 {@link CommandNode} 手工建树，
     * 再用这个重载注册；与 {@link #register(Object)} 的区别只是省掉了「传个 Object 让框架猜」
     * 这一步，编译期就能确认传进来的确实是一棵命令树。
     * </p>
     *
     * <pre>{@code
     * CommandNode root = CommandNode.literal("shop")
     *         .then(CommandNode.literal("buy")
     *                 .then(CommandNode.argument(Argument.integer("amount"))
     *                         .executes((sender, ctx) -> { ... })));
     *
     * commandManager.register(root);
     * }</pre>
     *
     * @param root 命令树根节点
     */
    void register(@NotNull CommandNode root);

        /**
     * 重新加载所有命令配置
     * <p>
     * 重新读取 commands.yml 并将配置应用到所有已注册的命令上。
     * 支持更新权限、描述、别名和启用状态。
     * </p>
     */
    void reload();
}
