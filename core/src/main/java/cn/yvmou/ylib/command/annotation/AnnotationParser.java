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
import java.util.List;

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
     * 解析 @SubCommand 注解的方法，构建命令树分支
     */
    private static void parseSubCommand(CommandNode root, Object instance, Method method, SubCommand annotation) {
        // 1. 获取或创建子命令节点
        CommandNode subCommandNode = getOrCreateSubCommandNode(root, annotation.value());

        // 2. 依次添加参数节点，并推进节点指针
        // 如果方法参数中有 @Optional，则在对应位置也绑定执行器
        appendArgumentNodesWithOptional(subCommandNode, instance, method);

        // 3. 配置权限与描述（在 subCommandNode 路径下的所有节点上生效）
        configureNode(subCommandNode, annotation);
    }

    /**
     * 获取或创建子命令字面量节点 (支持嵌套路径，如 "reload config")
     */
    private static CommandNode getOrCreateSubCommandNode(CommandNode root, String commandValue) {
        String subCommandName = commandValue.trim();
        
        // 支持主命令默认执行逻辑：当 @SubCommand("") 时，直接在 root 上操作
        if (subCommandName.isEmpty()) {
            return root;
        }
        
        // 支持多级子命令，例如 @SubCommand("reload config")
        String[] parts = subCommandName.split("\\s+");
        CommandNode currentNode = root;
        
        for (String part : parts) {
            CommandNode literalNode = CommandNode.literal(part);
            currentNode = getOrAddChild(currentNode, literalNode);
        }
        
        return currentNode;
    }

    /**
     * 解析并追加参数节点，同时处理 @Optional 逻辑
     */
    private static void appendArgumentNodesWithOptional(CommandNode startNode, Object instance, Method method) {
        CommandNode currentNode = startNode;
        Parameter[] parameters = method.getParameters();
        
        // 统计带有 @Arg 的参数（实际命令参数）
        boolean hasArgs = false;
        boolean firstArgIsOptional = false;
        
        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(Arg.class)) {
                hasArgs = true;
                if (param.isAnnotationPresent(Optional.class)) {
                    firstArgIsOptional = true;
                }
                // 只需要检查第一个找到的 @Arg 参数即可
                break;
            }
        }
        
        // 如果没有命令参数（只有 Sender/Context 等注入参数），或者第一个命令参数是可选的
        // 那么 startNode 必须绑定执行器
        if (!hasArgs || firstArgIsOptional) {
            startNode.executes(createExecutor(instance, method));
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!parameter.isAnnotationPresent(Arg.class)) {
                continue;
            }
            
            Arg argAnnotation = parameter.getAnnotation(Arg.class);
            Argument<?> argument = detectArgumentType(method, argAnnotation.value());
            if (argument == null) {
                argument = Argument.string(argAnnotation.value());
            }

            // 处理动态补全
            if (!argAnnotation.suggestion().isEmpty()) {
                SuggestionProvider provider = createSuggestionProvider(instance, argAnnotation.suggestion());
                if (provider != null) {
                    argument.suggests(provider);
                }
            }

            // 如果参数被标记为 @Optional，同步状态到 Argument 对象
            if (parameter.isAnnotationPresent(Optional.class)) {
                argument.optional();
            }

            CommandNode childNode = CommandNode.argument(argument);
            currentNode = getOrAddChild(currentNode, childNode);
            
            // 如果当前参数是可选的，或者它是链条的最后一个
            // 绑定执行器：即在该节点被输入后，命令可以正常结束
            if (i == parameters.length - 1 || (i + 1 < parameters.length && parameters[i + 1].isAnnotationPresent(Optional.class))) {
                currentNode.executes(createExecutor(instance, method));
            }
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
     * 根据方法参数类型推断 Argument 类型
     * 
     * @param method 目标方法
     * @param argName 参数名称（用于 Argument 命名）
     * @return 对应的 Argument 对象，无法推断时返回 null
     */
    private static Argument<?> detectArgumentType(Method method, String argName) {
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
