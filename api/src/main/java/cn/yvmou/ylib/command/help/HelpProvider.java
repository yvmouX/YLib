package cn.yvmou.ylib.command.help;

import cn.yvmou.ylib.command.annotation.Arg;
import cn.yvmou.ylib.command.annotation.Command;
import cn.yvmou.ylib.command.annotation.Optional;
import cn.yvmou.ylib.command.annotation.SubCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 从带注解的命令类自动生成帮助条目。
 *
 * <p>这样插件的 help <b>不需要手写清单</b>——清单迟早会和代码不一致，
 * 而描述本身就写在 {@code @Command} / {@code @SubCommand} 注解上。</p>
 *
 * <p>路径规则与 YLib 的注解解析保持一致：</p>
 * <ul>
 *   <li>注解在<b>方法</b>上：{@code @SubCommand("reload")} → {@code /cmd reload}；
 *       方法上的 {@code @Arg} 参数按声明顺序追加为 {@code <name>}，
 *       带 {@code @Optional} 的写成 {@code [name]}；</li>
 *   <li>注解在<b>嵌套类</b>上：作为分组，其内部方法的路径以该值为前缀，
 *       例如类上 {@code @SubCommand("task")} + 方法上 {@code @SubCommand("create")}
 *       → {@code /cmd task create}；</li>
 *   <li>{@code @SubCommand("")}：命令本身（无子命令），渲染为 {@code /cmd}。</li>
 * </ul>
 *
 * <h2>为什么组内要排序</h2>
 * {@code Class#getDeclaredMethods()} <b>不保证</b>返回声明顺序（实测同一类在不同 JVM 上
 * 顺序不同）。若直接使用，帮助条目的顺序会随运行环境变化——同一份代码在别人机器上
 * 看到的顺序可能完全不同。
 * <p>
 * 想按源码声明顺序展示就必须解析类文件字节码，代价与收益不成比例；
 * 因此这里选择：<b>分组顺序固定</b>（嵌套类的声明顺序在反射中是稳定的），
 * <b>组内命令按语法字典序</b>。结果在任何 JVM 上都一致，且按字母查找命令更方便。
 * 需要严格声明顺序时，可改用手工 {@code entry(...)} 逐条列出。
 *
 * <p>本类只读取注解，不产生任何副作用。</p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public final class HelpProvider {

    private HelpProvider() {
    }

    /**
     * 取命令的主名称（{@code @Command(name)}）。
     *
     * @return 主名称；类上没有 {@code @Command} 时返回 null
     */
    @Nullable
    public static String commandName(@NotNull Class<?> commandClass) {
        Command annotation = commandClass.getAnnotation(Command.class);
        return annotation == null ? null : annotation.name();
    }

    /** 取命令的主名称。 */
    @Nullable
    public static String commandName(@NotNull Object commandInstance) {
        return commandName(commandInstance.getClass());
    }

    /** 扫描命令类，生成帮助条目（含分组标题）。 */
    public static List<Entry> scan(@NotNull Class<?> commandClass) {
        List<Entry> entries = new ArrayList<Entry>();
        Command annotation = commandClass.getAnnotation(Command.class);
        if (annotation == null) {
            // 没有 @Command 就没有可用的命令前缀：返回空而不是抛异常，
            // 帮助渲染属于展示功能，不该因为标记缺失让调用方崩溃
            return entries;
        }
        collect(commandClass, "/" + annotation.name(), entries);
        return entries;
    }

    /** 扫描命令实例。 */
    public static List<Entry> scan(@NotNull Object commandInstance) {
        return scan(commandInstance.getClass());
    }

    /**
     * 递归收集：先处理嵌套类分组（其在反射中的顺序是稳定的声明顺序），
     * 再处理本类的方法（顺序不稳定，因此排序后输出）。
     */
    private static void collect(Class<?> clazz, String prefix, List<Entry> entries) {
        for (Class<?> nested : clazz.getDeclaredClasses()) {
            SubCommand groupAnnotation = nested.getAnnotation(SubCommand.class);
            if (groupAnnotation == null) {
                continue;
            }
            String groupName = groupAnnotation.description().isEmpty()
                    ? lastSegment(groupAnnotation.value())
                    : groupAnnotation.description();
            entries.add(Entry.group(groupName));
            collect(nested, join(prefix, groupAnnotation.value()), entries);
        }

        List<Entry> commands = new ArrayList<Entry>();
        for (Method method : clazz.getDeclaredMethods()) {
            SubCommand sub = method.getAnnotation(SubCommand.class);
            if (sub == null) {
                continue;
            }
            String permission = sub.permission().isEmpty() ? null : sub.permission();
            commands.add(Entry.command(appendArguments(join(prefix, sub.value()), method),
                    sub.description(), permission));
        }
        Collections.sort(commands, new Comparator<Entry>() {
            @Override
            public int compare(Entry left, Entry right) {
                return left.usage().compareToIgnoreCase(right.usage());
            }
        });
        entries.addAll(commands);
    }

    /**
     * 拼接路径。
     * <p>
     * {@code @SubCommand("")} 表示命令本身，此时只返回前缀。
     */
    private static String join(String prefix, String value) {
        if (value == null || value.isEmpty()) {
            return prefix;
        }
        return prefix + " " + value;
    }

    /** 取路径最后一段，用于分组标题缺省值（如 "task list" → "list"）。 */
    private static String lastSegment(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int index = path.lastIndexOf(' ');
        return index < 0 ? path : path.substring(index + 1);
    }

    /**
     * 把方法上的 {@code @Arg} 参数追加为语法中的占位符。
     * <p>
     * {@code @Arg} 显式命名优先，否则用编译期参数名（需要 {@code -parameters}）；
     * 两者都拿不到时退化为「参数N」，保证语法行始终可读。
     */
    private static String appendArguments(String usage, Method method) {
        StringBuilder builder = new StringBuilder(usage);
        int index = 0;
        for (Parameter parameter : method.getParameters()) {
            index++;
            // 只把 @Arg 参数计入语法：CommandSender / CommandContext 是注入的，不出现在命令里
            Arg arg = parameter.getAnnotation(Arg.class);
            if (arg == null) {
                continue;
            }
            String name = arg.value();
            if (name == null || name.isEmpty()) {
                name = parameter.getName();
            }
            if (name == null || name.trim().isEmpty() || name.startsWith("arg")) {
                // 未开 -parameters 时 parameter.getName() 形如 arg0，对用户没有意义
                name = "参数" + index;
            }
            boolean optional = parameter.isAnnotationPresent(Optional.class);
            builder.append(' ').append(optional ? '[' : '<').append(name).append(optional ? ']' : '>');
        }
        return builder.toString();
    }

    /** 帮助条目：分组标题或命令。 */
    public static final class Entry {

        private final String group;
        private final String usage;
        private final String description;
        private final String permission;

        private Entry(String group, String usage, String description, String permission) {
            this.group = group;
            this.usage = usage;
            this.description = description;
            this.permission = permission;
        }

        static Entry group(String name) {
            return new Entry(name, null, null, null);
        }

        static Entry command(String usage, String description, String permission) {
            return new Entry(null, usage, description, permission);
        }

        /** 分组名；命令条目返回 null。 */
        public String group() {
            return group;
        }

        /** 完整语法，如 {@code /ptx claim <id>}；分组标题返回 null。 */
        public String usage() {
            return usage;
        }

        /** 描述，可能为 null。 */
        public String description() {
            return description;
        }

        /** 所需权限，可能为 null。 */
        public String permission() {
            return permission;
        }

        public boolean isCommand() {
            return usage != null;
        }
    }
}
