package cn.yvmou.ylib.api.command.tree;

import cn.yvmou.ylib.api.command.args.Argument;
import cn.yvmou.ylib.api.command.args.SuggestionProvider;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 命令节点 - 构建命令树的核心组件
 * <pre>
 *     此类用于构建命令树结构，支持两种类型的节点：
 *     1. Literal节点：固定字符串，如 "help", "give"
 *     2. Argument节点：参数占位符，如 Arguments.player("target"), Arguments.integer("amount")
 * </pre>
 * <pre>{@code
 *   CommandNode.literal("give")
 *     .then(CommandNode.argument(Arguments.player("target"))
 *         .then(CommandNode.argument(Arguments.item("item"))
 *             .executes(context -> {
 *                 // 执行逻辑
 *                 return 1;
 *             })))
 * }
 */
public class CommandNode {
    /**
     * 字面量名称 - 仅对literal节点有效
     * 例如："help", "give", "set"
     * 当此值不为null时，表示这是一个literal节点
     */
    private final String literal;
    
    /**
     * 参数定义 - 仅对argument节点有效
     * 例如：Arguments.player("target"), Arguments.integer("amount")
     * 当此值不为null时，表示这是一个argument节点
     */
    private final Argument<?> argument;
    
    /**
     * 子节点列表 - 构建命令树的关键
     * 用于实现命令的多级结构，如 /give <player> <item> <amount>
     * 每个子节点代表一个可能的下级命令或参数
     */
    private final List<CommandNode> children = new ArrayList<>();
    
    /**
     * 命令执行器 - 当命令匹配到此节点时执行
     * 如果为null，表示这是一个中间节点，需要继续匹配子节点
     */
    private CommandExecutor executor;
    
    /**
     * 执行要求 - 自定义条件判断
     * 例如：仅允许OP执行、需要特定游戏状态等
     * 如果返回false，命令将不会被执行
     */
    private Predicate<CommandSender> requirement;
    
    /**
     * 权限节点 - Bukkit权限系统
     * 例如："ylib.command.give", "ylib.admin"
     * 玩家必须拥有对应权限才能执行此命令
     */
    private String permission;
    
    /**
     * 命令描述 - 用于帮助信息或命令列表
     * 例如："给予玩家物品", "设置玩家等级"
     */
    private String description;
    
    /**
     * 命令别名列表 - 用于替代主命令名
     * 例如：主命令是 "home"，别名可以是 ["h", "house", "residence"]
     * 别名在注册时会被注册为独立的命令，但指向相同的执行逻辑
     */
    private List<String> aliases;

    // 私有构造，通过工厂方法创建
    private CommandNode(String literal, Argument<?> argument) {
        this.literal = literal;
        this.argument = argument;
    }

    // ========== 工厂方法 ==========

    @Contract("_ -> new")
    public static @NotNull CommandNode literal(String name) {
        return new CommandNode(name, null);
    }

    @Contract("_ -> new")
    public static @NotNull CommandNode argument(Argument<?> argument) {
        return new CommandNode(null, argument);
    }

    // ========== 配置方法 ==========

    /**
     * 添加子节点 - 构建命令树的关键方法
     *
     * 用于构建多级命令结构，支持任意深度的嵌套
     * 例如：/give <player> <item> <amount>
     *
     * 示例：
     * literal("give").then(argument(player("target")).then(argument(item("item"))))
     *
     * @param child 子节点，可以是literal或argument节点
     * @return 当前节点，支持链式调用
     */
    @SuppressWarnings("UnusedReturnValue")
    public CommandNode then(CommandNode child) {
        this.children.add(child);
        return this;
    }

    /**
     * 设置命令执行器 - 当匹配到此节点时执行
     * <p>
     * 执行器负责实际的业务逻辑，如给予物品、传送玩家等
     * 如果没有设置执行器，此节点只能作为中间节点
     *
     * @param executor 命令执行逻辑
     * @return 当前节点，支持链式调用
     */
    @SuppressWarnings("UnusedReturnValue")
    public CommandNode executes(CommandExecutor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * 设置执行要求 - 自定义条件判断
     * <p>
     * 用于实现复杂的权限控制，如：
     * - 仅OP可执行：requires(sender -> sender.isOp())
     * - 需要特定权限：requires(sender -> sender.hasPermission("ylib.admin"))
     * - 游戏状态检查：requires(sender -> game.isStarted())
     * <p>
     * 注意：此方法与permission()不同，permission()使用Bukkit的权限系统
     *
     * @param requirement 条件判断函数，返回true表示允许执行
     * @return 当前节点，支持链式调用
     */
    public CommandNode requires(Predicate<CommandSender> requirement) {
        this.requirement = requirement;
        return this;
    }

    /**
     * 设置权限节点 - Bukkit权限系统
     * <p>
     * 使用Bukkit的权限系统，玩家必须拥有指定权限才能执行
     * 权限检查在执行器之前进行
     * <p>
     * 示例：
     * permission("ylib.command.give") - 需要 ylib.command.give 权限
     * permission("ylib.admin") - 需要管理员权限
     * 
     * @param permission 权限字符串，如 "ylib.command.give"
     * @return 当前节点，支持链式调用
     */
    public CommandNode permission(String permission) {
        this.permission = permission;
        return this;
    }
    
    /**
     * 设置命令描述 - 用于帮助信息
     * <p>
     * 描述信息可用于：
     * - 命令帮助：/help give
     * - 命令列表：显示每个命令的作用
     * - 错误提示：当命令执行失败时显示
     * 
     * @param description 命令描述，如 "给予玩家物品"
     * @return 当前节点，支持链式调用
     */
    public CommandNode description(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * 设置命令别名 - 替代主命令名（变参版本）
     * 
     * 更简洁的设置别名方式，直接传入多个字符串
     * 例如：aliases("h", "house", "residence")
     * 
     * **注意：此方法仅对literal节点有效，argument节点不支持别名**
     * 
     * @param aliases 可变参数，多个别名
     * @return 当前节点，支持链式调用
     * @throws IllegalStateException 如果是argument节点调用此方法
     */
    public CommandNode aliases(String... aliases) {
        if (isArgument()) {
            throw new IllegalStateException("Argument nodes do not support aliases. Only literal nodes can have aliases.");
        }
        this.aliases = Arrays.asList(aliases);
        return this;
    }

    // ========== Getters ==========

    /**
     * 判断是否为字面量节点
     * 
     * @return 如果是literal节点返回true，否则返回false
     */
    public boolean isLiteral() {
        return literal != null;
    }

    /**
     * 判断是否为参数节点
     * 
     * @return 如果是argument节点返回true，否则返回false
     */
    public boolean isArgument() {
        return argument != null;
    }

    /**
     * 获取字面量名称
     * 
     * @return 字面量名称，如果是argument节点返回null
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * 获取参数定义
     * 
     * @return 参数定义，如果是literal节点返回null
     */
    public Argument<?> getArgument() {
        return argument;
    }

    /**
     * 获取所有子节点
     * 
     * @return 子节点列表，可用于遍历命令树结构
     */
    public List<CommandNode> getChildren() {
        return children;
    }

    /**
     * 获取命令执行器
     * 
     * @return 执行器，如果为null表示这是中间节点
     */
    public CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * 获取执行要求
     * 
     * @return 条件判断函数，如果为null表示没有特殊要求
     */
    public Predicate<CommandSender> getRequirement() {
        return requirement;
    }

    /**
     * 获取权限节点
     * 
     * @return 权限字符串，如果为null表示不需要特殊权限
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * 获取命令描述
     * 
     * @return 描述信息，如果为null表示没有设置描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取命令别名列表
     * 
     * @return 别名列表，如果为null表示没有设置别名
     */
    public List<String> getAliases() {
        return aliases;
    }
}
