package cn.yvmou.ylib.command;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.command.args.Argument;
import cn.yvmou.ylib.command.context.CommandContext;
import cn.yvmou.ylib.command.exception.CommandParseException;
import cn.yvmou.ylib.command.exception.CommandValidationException;
import cn.yvmou.ylib.command.tree.CommandNode;
import org.bukkit.command.CommandSender;

import java.util.*;

public class CommandDispatcher {
    // 当前传入的 root 命令节点已应用配置(commands.yml)覆盖（别名、描述、权限）
    public void execute(CommandNode root, CommandSender sender, String[] args, String label) throws Exception {
        Map<String, Object> parsedArgs = new HashMap<>();
        List<Argument<?>> parsedArguments = new ArrayList<>(); // Track parsed arguments for validation
        CommandNode currentNode = root;
        int argIndex = 0;

        // 首先对根节点命令进行权限和需求检查
        if (!validatePermissionAndConditions(sender, root)) return;

        // 遍历参数
        while (argIndex < args.length) {
            String currentArg = args[argIndex];
            boolean matched = false;

            // 查找匹配的子节点
            for (CommandNode child : currentNode.getChildren()) {
                // 1. 匹配 Literal
                if (child.isLiteral() && child.getLiteral().equalsIgnoreCase(currentArg)) {
                    // 权限和需求检查
                    if (!validatePermissionAndConditions(sender, root)) return;
                    
                    currentNode = child;
                    matched = true;
                    argIndex++;
                    break;
                }
                
                // 2. 匹配 Argument
                if (child.isArgument()) {
                    try {
                        // 权限和需求检查
                        if (!validatePermissionAndConditions(sender, root)) return;

                        // 解析参数
                        Argument<?> argument = child.getArgument();
                        Object value = argument.parse(sender, currentArg);
                        parsedArgs.put(argument.getName(), value);
                        parsedArguments.add(argument); // Add to list
                        
                        currentNode = child;
                        matched = true;
                        argIndex++;
                        break;
                    } catch (CommandParseException ignored) {
                        // 如果解析失败，尝试下一个子节点（可能是 Literal 或其他 Argument）
                    }
                }
            }

            // 但凡有一个字面量/参数没有匹配成功，就抛出未知参数错误，退出整个循环并终止代码，而不是继续匹配。
            if (!matched) {
                // 如果当前节点有 Executor 且参数已用尽，则执行（但这在 while 循环里通常意味着参数多余）
                // 这里简单处理：抛出未知命令/参数错误
                throw new CommandParseException("未知参数: " + currentArg);
            }
        }

        // 所有参数处理完毕，检查当前节点是否有 Executor
        if (currentNode.getExecutor() == null) {
            throw new CommandParseException("命令未完成"); // 这通常意味着参数不足
        }

        // 构建上下文
        CommandContext context = new CommandContext(sender, parsedArgs, args, label);
        
        // 执行验证器 (Post-parsing validation)
        validateArguments(context, parsedArguments);

        // 执行命令
        currentNode.getExecutor().execute(sender, context);
    }

    public List<String> tabComplete(CommandNode root, CommandSender sender, String[] args) {
        CommandNode currentNode = root;
        int argIndex = 0;

        // 定位到最后一个匹配的节点
        while (argIndex < args.length - 1) {
            String currentArg = args[argIndex];
            boolean matched = false;

            for (CommandNode child : currentNode.getChildren()) {
                if (child.isLiteral() && child.getLiteral().equalsIgnoreCase(currentArg)) {
                    if (hasPermission(sender, child)) {
                        currentNode = child;
                        matched = true;
                        break;
                    }
                }
                if (child.isArgument()) {
                    try {
                        child.getArgument().parse(sender, currentArg);
                        if (hasPermission(sender, child)) {
                            currentNode = child;
                            matched = true;
                            break;
                        }
                    } catch (CommandParseException ignored) {
                    }
                }
            }

            if (!matched) {
                return Collections.emptyList();
            }
            argIndex++;
        }

        // 当前 args[args.length - 1] 是正在输入的参数
        String currentInput = args[args.length - 1];
        List<String> completions = new ArrayList<>();

        // 临时 Context，仅包含前面的参数
        // 注意：这里无法获取完整的 parsedArgs，因为前面的参数可能没解析保存
        // 为了简化，Tab补全时的 Context 可能不包含参数值，或者需要重构解析逻辑以支持部分解析
        CommandContext partialContext = new CommandContext(sender, Collections.emptyMap(), args, "");

        for (CommandNode child : currentNode.getChildren()) {
            if (!hasPermission(sender, child)) continue;

            if (child.isLiteral()) {
                if (child.getLiteral().toLowerCase().startsWith(currentInput.toLowerCase())) {
                    completions.add(child.getLiteral());
                }
            } else if (child.isArgument()) {
                // 参数补全
                completions.addAll(child.getArgument().suggest(sender, partialContext, currentInput));
            }
        }

        return completions;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */

    private boolean validatePermissionAndConditions(CommandSender sender, CommandNode node) {
        if (!hasPermission(sender, node)) {
            YLib._getLogger().to(sender).error("没有权限执行此命令");
            return false;
        }
        if (!hasRequirement(sender, node)) {
            YLib._getLogger().to(sender).error("没有满足命令需求");
            return false;
        }
        return true;
    }

    // 当node中没有权限配置时，默认返回true
    private boolean hasPermission(CommandSender sender, CommandNode node) {
        if (node.getPermission() != null && !node.getPermission().isEmpty()) {
            return sender.hasPermission(node.getPermission());
        }
        return true;
    }

    // 当node中没有需求配置时，默认返回true
    private boolean hasRequirement(CommandSender sender, CommandNode node) {
        if (node.getRequirement() != null) {
            return node.getRequirement().test(sender);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(CommandContext context, List<Argument<?>> arguments) throws CommandValidationException {
        for (Argument<?> argument : arguments) {
            Object value = context.get(argument.getName());
            if (value != null) {
                // 强制转换以调用 validate，因为我们在 Argument 类中定义了 validate(Context, T)
                // 这里利用泛型擦除，虽然有风险但由于 value 是由 parse 产生的，类型应该匹配
                ((Argument<Object>) argument).validate(context, value);
            }
        }
    }
}
