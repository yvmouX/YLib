package cn.yvmou.ylib.command.annotation;

import cn.yvmou.ylib.command.args.Argument;
import cn.yvmou.ylib.command.context.CommandContext;
import cn.yvmou.ylib.command.tree.CommandExecutor;
import cn.yvmou.ylib.command.tree.CommandNode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 注解解析器，将 @Command 标注的类转换为 CommandNode 树
 */
public class AnnotationParser {

    public CommandNode parse(Object commandInstance) {
        Class<?> clazz = commandInstance.getClass();
        Command commandAnnotation = clazz.getAnnotation(Command.class);

        if (commandAnnotation == null) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Command");
        }

        // 创建根节点
        CommandNode root = CommandNode.literal(commandAnnotation.name())
                .aliases(commandAnnotation.aliases())
                .description(commandAnnotation.description())
                .permission(commandAnnotation.permission());


        // 解析方法
        for (Method method : clazz.getDeclaredMethods()) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            if (subCommand != null) {
                parseSubCommand(root, commandInstance, method, subCommand);
            }
        }

        return root;
    }


    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    private void parseSubCommand(CommandNode root, Object instance, Method method, SubCommand annotation) {
        /*
          私人笔记

          trim()方法
          移除字符串首尾的所有空白字符（包括空格、制表符、换行符等），返回处理后的新字符串；中间的空白字符会保留，原字符串因不可变不会被修改；若字符串全为空白 / 空，返回空字符串。
          该方法常用于用户输入、命令参数等场景的容错处理，比如清理命令字符串首尾意外输入的空格，同时保留参数间的分隔空格，避免解析失败。
         */
        String path = annotation.value().trim();
        // 当使用SubCommand注解的方法的value为""时，parts会是空数组，便不会进入for循环，直接将当前方法作为根节点的执行器。
        // 否则 按「一个或多个空白字符」拆分字符串为数组
        String[] parts = path.isEmpty() ? new String[0] : path.split("\\s+");

        CommandNode currentNode = root;

        // 1. 构建路径节点
        Map<String, Argument<?>> methodArguments = new HashMap<>();
        for (String part : parts) {
            CommandNode childNode;
            if (part.startsWith("<") && part.endsWith(">")) {
                /// 必需参数 <arg>
                String argName = part.substring(1, part.length() - 1);
                Argument<?> argument = detectArgumentType(method, argName);
                if (argument == null) {
                    // 如果无法自动检测，默认为 String
                    argument = Argument.string(argName);
                }
                childNode = CommandNode.argument(argument);
                methodArguments.put(argName, argument);
            } else if (part.startsWith("[") && part.endsWith("]")) {
                /// 可选参数 [arg] - 暂不支持复杂的可选参数逻辑，简化处理为 String 且 Optional
                String argName = part.substring(1, part.length() - 1);
                Argument<?> argument = detectArgumentType(method, argName);
                if (argument == null) {
                    argument = Argument.string(argName);
                }
                argument.optional();
                childNode = CommandNode.argument(argument);
                methodArguments.put(argName, argument);
            } else {
                // 字面量
                childNode = CommandNode.literal(part);
            }

            // 查找是否已存在相同的子节点
            CommandNode existing = findChild(currentNode, childNode);
            if (existing != null) {
                currentNode = existing;
            } else {
                currentNode.then(childNode);
                currentNode = childNode;
            }
        }

        // 2. 设置执行器
        currentNode.permission(annotation.permission());
        if (!annotation.description().isEmpty()) {
            currentNode.description(annotation.description());
        }

        currentNode.executes(createExecutor(instance, method));
    }

    private CommandNode findChild(CommandNode parent, CommandNode target) {
        for (CommandNode child : parent.getChildren()) {
            if (target.isLiteral() && child.isLiteral() && target.getLiteral().equalsIgnoreCase(child.getLiteral())) {
                return child;
            }
            if (target.isArgument() && child.isArgument() && target.getArgument().getName().equals(child.getArgument().getName())) {
                return child;
            }
        }
        return null;
    }

    // 推断参数类型
    private Argument<?> detectArgumentType(Method method, String argName) {
        for (Parameter parameter : method.getParameters()) {
            Arg argAnnotation = parameter.getAnnotation(Arg.class);
            if (argAnnotation != null && argAnnotation.value().equals(argName)) {
                return createArgumentForType(parameter.getType(), argName);
            }
            // 如果参数名匹配（需要编译时开启 -parameters，这里作为备选）
            if (parameter.getName().equals(argName)) {
                return createArgumentForType(parameter.getType(), argName);
            }
        }
        return null;
    }

    private Argument<?> createArgumentForType(Class<?> type, String name) {
        if (type == int.class || type == Integer.class) return Argument.integer(name);
        if (type == double.class || type == Double.class) return Argument.number(name);
        if (type == boolean.class || type == Boolean.class) return Argument.bool(name);
        if (type == String.class) return Argument.string(name);
        if (type == Player.class) return Argument.player(name);
        if (type == World.class) return Argument.world(name);
        if (type.isEnum()) return Argument.enumValue(name, (Class<? extends Enum>) type);
        return Argument.string(name); // Default fallback
    }

    private CommandExecutor createExecutor(Object instance, Method method) {
        return (sender, context) -> {
            Object[] args = new Object[method.getParameterCount()];
            Parameter[] parameters = method.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> type = param.getType();

                if (CommandSender.class.isAssignableFrom(type)) {
                    args[i] = sender;
                } else if (CommandContext.class.isAssignableFrom(type)) {
                    args[i] = context;
                } else {
                    Arg argAnnotation = param.getAnnotation(Arg.class);
                    String argName = (argAnnotation != null) ? argAnnotation.value() : param.getName();

                    Object value = context.get(argName);
                    if (value == null && !param.isAnnotationPresent(org.jetbrains.annotations.Nullable.class)) {
                        // 尝试按类型获取（如果有且仅有一个该类型的参数）- 简化处理：忽略
                    }
                    args[i] = value;
                }
            }

            method.setAccessible(true);
            method.invoke(instance, args);
        };
    }
}