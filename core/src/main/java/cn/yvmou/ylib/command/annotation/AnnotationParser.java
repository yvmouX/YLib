package cn.yvmou.ylib.command.annotation;

import cn.yvmou.ylib.YLib;
import cn.yvmou.ylib.command.args.Argument;
import cn.yvmou.ylib.command.args.SuggestionProvider;
import cn.yvmou.ylib.command.context.CommandContext;
import cn.yvmou.ylib.command.tree.CommandExecutor;
import cn.yvmou.ylib.command.tree.CommandNode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 注解解析器，将 @Command 标注的类转换为 CommandNode 树
 */
public class AnnotationParser {

    // 工具类私有构造
    private AnnotationParser() {}

    public static CommandNode parse(Object commandInstance) {
        Class<?> clazz = commandInstance.getClass();
        Command commandAnnotation = clazz.getAnnotation(Command.class);

        if (commandAnnotation == null) {
            // 如果传入的不是主命令类（例如可能是嵌套的子命令类实例），尝试查找 SubCommand 注解
            // 但为了兼容性，通常 parse 方法入口还是应该只接受 @Command
            throw new IllegalArgumentException("Class " + clazz.getName() + " is not annotated with @Command");
        }

        // 创建根节点
        CommandNode root = CommandNode.literal(commandAnnotation.name())
                .aliases(commandAnnotation.aliases())
                .description(commandAnnotation.description())
                .permission(commandAnnotation.permission());

        // 解析类中的所有命令组件（方法和嵌套类）
        parseClassComponents(root, commandInstance);

        return root;
    }

    /**
     * 递归解析类中的所有 @SubCommand 标注的组件（方法和内部类）
     */
    private static void parseClassComponents(CommandNode root, Object instance) {
        Class<?> clazz = instance.getClass();

        // 1. 解析嵌套类 (Nested Classes) 作为分组节点
        for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
            SubCommand subCommand = nestedClass.getAnnotation(SubCommand.class);
            if (subCommand != null && !Modifier.isStatic(nestedClass.getModifiers())) {
                try {
                    // 实例化非静态内部类需要外部类实例
                    Constructor<?> constructor = nestedClass.getDeclaredConstructor(clazz);
                    constructor.setAccessible(true);
                    Object nestedInstance = constructor.newInstance(instance);
                    
                    // 获取或创建分组节点
                    CommandNode groupNode = getOrCreateSubCommandNode(root, subCommand.value());
                    configureNode(groupNode, subCommand);
                    
                    // 递归解析嵌套类的内容
                    parseClassComponents(groupNode, nestedInstance);
                } catch (Exception e) {
                    logError("Failed to instantiate nested command class: " + nestedClass.getName(), e);
                }
            } else if (subCommand != null && Modifier.isStatic(nestedClass.getModifiers())) {
                 try {
                    // 实例化静态嵌套类
                    Constructor<?> constructor = nestedClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Object nestedInstance = constructor.newInstance();
                    
                    // 获取或创建分组节点
                    CommandNode groupNode = getOrCreateSubCommandNode(root, subCommand.value());
                    configureNode(groupNode, subCommand);
                    
                    // 递归解析嵌套类的内容
                    parseClassComponents(groupNode, nestedInstance);
                } catch (Exception e) {
                    logError("Failed to instantiate nested command class: " + nestedClass.getName(), e);
                }
            }
        }

        // 2. 解析方法 (Methods) 作为执行节点
        for (Method method : clazz.getDeclaredMethods()) {
            SubCommand subCommand = method.getAnnotation(SubCommand.class);
            if (subCommand != null) {
                parseSubCommand(root, instance, method, subCommand);
            }
        }
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    /**
     * 解析 @SubCommand 注解的方法，构建命令树分支。
     * <p>
     * 路径支持两种写法（可混用）：
     * </p>
     * <ul>
     *   <li>纯字面量：{@code @SubCommand("give")}——方法上带 {@code @Arg} 的参数按声明顺序追加为参数节点</li>
     *   <li>带占位符：{@code @SubCommand("give <player> <item>")}——占位符段按名称绑定到方法上
     *       对应的 {@code @Arg} 参数（类型、补全、可选性随参数）；路径中未出现的 {@code @Arg}
     *       参数仍按声明顺序追加在路径末端</li>
     * </ul>
     */
    private static void parseSubCommand(CommandNode root, Object instance, Method method, SubCommand annotation) {
        // 1. 沿路径获取/创建节点；<占位符> 段绑定到方法上对应的 @Arg 参数，并记录已消费的参数名
        Set<String> consumedArgs = new HashSet<>();
        CommandNode subCommandNode = getOrCreateSubCommandNode(root, annotation.value(), instance, method, consumedArgs);

        // 2. 追加路径中未出现的 @Arg 参数节点，并沿节点链绑定执行器
        bindArgumentsAndExecutor(subCommandNode, instance, method, consumedArgs);

        // 3. 配置权限与描述（在 subCommandNode 路径下的所有节点上生效）
        configureNode(subCommandNode, annotation);
    }

    /**
     * 获取或创建子命令节点 (支持嵌套路径，如 "reload config" 与参数占位符 "give <player> <item>")
     * <p>
     * 此重载供嵌套类分组节点使用：路径中的 {@code <name>} 占位符段会降级为字符串参数节点
     * （分组节点没有对应方法参数可绑定；子方法的 @Arg 参数仍可通过上下文按名取值）。
     * </p>
     */
    private static CommandNode getOrCreateSubCommandNode(CommandNode root, String commandValue) {
        return getOrCreateSubCommandNode(root, commandValue, null, null, null);
    }

    /**
     * 获取或创建子命令节点，路径按空格分段：字面量段创建/复用字面量节点；
     * {@code <name>} 占位符段创建参数节点，类型与补全取自 method 上对应的
     * {@code @Arg} 参数（其次按参数名匹配），并把参数名记入 consumedArgs。
     */
    private static CommandNode getOrCreateSubCommandNode(CommandNode root, String commandValue,
                                                         Object instance, Method method, Set<String> consumedArgs) {
        String subCommandName = commandValue.trim();

        // 支持主命令默认执行逻辑：当 @SubCommand("") 时，直接在 root 上操作
        if (subCommandName.isEmpty()) {
            return root;
        }

        // 支持多级子命令与参数占位符，例如 @SubCommand("reload config")、@SubCommand("give <player> <item>")
        String[] parts = subCommandName.split("\\s+");
        CommandNode currentNode = root;

        for (String part : parts) {
            if (isPlaceholder(part)) {
                String argName = part.substring(1, part.length() - 1);
                Argument<?> argument = null;
                if (method != null) {
                    argument = buildArgumentForParam(instance, method, argName);
                    if (argument != null) {
                        consumedArgs.add(argName);
                    } else {
                        logWarn("Placeholder <" + argName + "> in @SubCommand(\"" + commandValue
                                + "\") does not match any parameter of "
                                + method.getDeclaringClass().getSimpleName() + "#" + method.getName()
                                + ", falling back to a string argument");
                    }
                }
                if (argument == null) {
                    argument = Argument.string(argName);
                }
                currentNode = getOrAddChild(currentNode, CommandNode.argument(argument));
            } else {
                currentNode = getOrAddChild(currentNode, CommandNode.literal(part));
            }
        }

        return currentNode;
    }

    private static boolean isPlaceholder(String segment) {
        return segment.length() > 2 && segment.startsWith("<") && segment.endsWith(">");
    }

    /**
     * 在路径节点下追加未消费的 @Arg 参数节点，并沿节点链绑定执行器。
     * <p>
     * 执行器绑定规则——命令必须在每个「可以到此结束」的节点上可执行：
     * 最后一个参数节点、后继参数为可选参数的节点，以及链首参数为可选时的路径节点本身。
     * 路径占位符已消费全部参数时，执行器绑定在路径末端节点上。
     * </p>
     */
    private static void bindArgumentsAndExecutor(CommandNode startNode, Object instance, Method method, Set<String> consumedArgs) {
        List<CommandNode> chain = new ArrayList<>();
        CommandNode currentNode = startNode;

        for (Parameter parameter : method.getParameters()) {
            Arg argAnnotation = parameter.getAnnotation(Arg.class);
            if (argAnnotation == null || consumedArgs.contains(argAnnotation.value())) {
                continue;
            }
            Argument<?> argument = buildArgumentForParam(instance, method, argAnnotation.value());
            if (argument == null) {
                argument = Argument.string(argAnnotation.value());
            }
            currentNode = getOrAddChild(currentNode, CommandNode.argument(argument));
            chain.add(currentNode);
        }

        if (chain.isEmpty()) {
            // 无命令参数（或全部由路径占位符消费）：路径末端即可执行
            startNode.executes(createExecutor(instance, method));
            return;
        }

        for (int i = 0; i < chain.size(); i++) {
            boolean last = i == chain.size() - 1;
            boolean nextOptional = !last && chain.get(i + 1).getArgument().isOptional();
            if (last || nextOptional) {
                chain.get(i).executes(createExecutor(instance, method));
            }
        }
        // 链首参数可选：不带任何参数也应能执行
        if (chain.get(0).getArgument().isOptional()) {
            startNode.executes(createExecutor(instance, method));
        }
    }

    /**
     * 获取已存在的子节点，如果不存在则添加并返回新节点
     */
    private static CommandNode getOrAddChild(CommandNode parent, CommandNode child) {
        // 查找已存在的同名子节点（字面量或参数名匹配）
        for (CommandNode existingChild : parent.getChildren()) {
            if (child.isLiteral() && existingChild.isLiteral() && 
                child.getLiteral().equalsIgnoreCase(existingChild.getLiteral())) {
                return existingChild;
            }
            if (child.isArgument() && existingChild.isArgument() && 
                child.getArgument().getName().equals(existingChild.getArgument().getName())) {
                return existingChild;
            }
        }
        
        // 未找到，则添加新节点
        parent.then(child);
        return child;
    }

    /**
     * 配置节点的通用属性（权限、描述）
     */
    private static void configureNode(CommandNode node, SubCommand annotation) {
        if (!annotation.permission().isEmpty()) {
            node.permission(annotation.permission());
        }
        if (!annotation.description().isEmpty()) {
            node.description(annotation.description());
        }
    }

    /**
     * 为指定参数名构建参数定义：优先匹配 {@code @Arg(value)} 参数，其次按参数名匹配
     * （需编译时开启 -parameters）。类型取自参数类型，补全取自 {@code @Arg(suggestion)}，
     * 可选性取自 {@code @Optional}。找不到对应参数时返回 null。
     */
    private static Argument<?> buildArgumentForParam(Object instance, Method method, String argName) {
        for (Parameter parameter : method.getParameters()) {
            Arg argAnnotation = parameter.getAnnotation(Arg.class);
            boolean byAnnotation = argAnnotation != null && argAnnotation.value().equals(argName);
            boolean byName = argAnnotation == null && parameter.getName().equals(argName);
            if (!byAnnotation && !byName) {
                continue;
            }
            Argument<?> argument = createArgumentForType(parameter.getType(), argName);
            if (argAnnotation != null && !argAnnotation.suggestion().isEmpty()) {
                SuggestionProvider provider = createSuggestionProvider(instance, argAnnotation.suggestion());
                if (provider != null) {
                    argument.suggests(provider);
                }
            }
            if (parameter.isAnnotationPresent(Optional.class)) {
                argument.optional();
            }
            return argument;
        }
        return null;
    }

    private static Argument<?> createArgumentForType(Class<?> type, String argName) {
        if (type == int.class || type == Integer.class) return Argument.integer(argName);
        if (type == double.class || type == Double.class) return Argument.number(argName);
        if (type == boolean.class || type == Boolean.class) return Argument.bool(argName);
        if (type == String.class) return Argument.string(argName);
        if (type == Player.class) return Argument.player(argName);
        if (type == World.class) return Argument.world(argName);
        if (type.isEnum()) return Argument.enumValue(argName, (Class<? extends Enum>) type);
        return Argument.string(argName); // Default fallback
    }

    /**
     * 创建命令执行器，负责参数注入与反射调用
     */
    private static CommandExecutor createExecutor(Object instance, Method method) {
        return (sender, context) -> {
            Object[] args = new Object[method.getParameterCount()];
            Parameter[] parameters = method.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];
                Class<?> type = param.getType();

                // 注入 CommandSender
                if (CommandSender.class.isAssignableFrom(type)) {
                    args[i] = sender;
                } 
                // 注入 CommandContext
                else if (CommandContext.class.isAssignableFrom(type)) {
                    args[i] = context;
                } 
                // 注入 @Arg 参数值
                else {
                    Arg argAnnotation = param.getAnnotation(Arg.class);
                    String argName = (argAnnotation != null) ? argAnnotation.value() : param.getName();

                    // 尝试从 context 中获取值，如果未提供则为 null
                    Object value = null;
                    try {
                        value = context.get(argName);
                    } catch (Exception ignored) {
                        // 可能该参数未被输入，忽略异常
                    }
                    
                    // 如果值为 null，且参数是基本类型，则提供默认值
                    if (value == null && type.isPrimitive()) {
                        if (type == int.class) value = 0;
                        else if (type == double.class) value = 0.0;
                        else if (type == float.class) value = 0.0f;
                        else if (type == long.class) value = 0L;
                        else if (type == boolean.class) value = false;
                        else if (type == short.class) value = (short) 0;
                        else if (type == byte.class) value = (byte) 0;
                        else if (type == char.class) value = '\u0000';
                    }
                    
                    args[i] = value;
                }
            }

            try {
                method.setAccessible(true);
                method.invoke(instance, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                } else {
                    throw e;
                }
            }
        };
    }

    /**
     * 创建动态补全提供器
     */
    private static SuggestionProvider createSuggestionProvider(Object instance, String methodName) {
        try {
            Method method = instance.getClass().getDeclaredMethod(methodName, CommandSender.class, CommandContext.class, String.class);
            method.setAccessible(true);
            
            return (sender, context, currentInput) -> {
                try {
                    return (List<String>) method.invoke(instance, sender, context, currentInput);
                } catch (Exception e) {
                    logError("Suggestion provider '" + methodName + "' failed", e);
                    return java.util.Collections.emptyList();
                }
            };
        } catch (NoSuchMethodException e) {
            // 尝试查找无参方法作为备选（虽然不太标准，但有时方便）
            try {
                Method method = instance.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                return (sender, context, currentInput) -> {
                    try {
                        return (List<String>) method.invoke(instance);
                    } catch (Exception ex) {
                        logError("Suggestion provider '" + methodName + "' failed", ex);
                        return java.util.Collections.emptyList();
                    }
                };
            } catch (NoSuchMethodException ex) {
                // 方法未找到
                logWarn("Suggestion method '" + methodName + "' not found in " + instance.getClass().getName());
                return null;
            }
        }
    }

    private static void logError(String message, Throwable t) {
        try {
            YLib.getYLib().getLogger().error("{}", message, t);
        } catch (IllegalStateException ignored) {
            // YLib 未初始化时（命令解析阶段理论上不会发生），回退到标准错误输出
            System.err.println("[YLib] " + message);
        }
    }

    private static void logWarn(String message) {
        try {
            YLib.getYLib().getLogger().warn("{}", message);
        } catch (IllegalStateException ignored) {
            System.err.println("[YLib] " + message);
        }
    }
}
