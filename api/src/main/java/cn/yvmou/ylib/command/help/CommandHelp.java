package cn.yvmou.ylib.command.help;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一的 help 输出构建器。
 *
 * <p>目的：让所有依赖 YLib 的插件拥有<b>外观一致</b>的命令帮助，而不是各写一套。</p>
 *
 * <p>统一样式：</p>
 * <pre>
 * &amp;8&amp;m----------&amp;r &amp;6命令帮助 &amp;8&amp;m----------
 * &amp;7用法
 * &amp;8» &amp;f/ptx &amp;7打开界面
 * &amp;8» &amp;f/ptx claim &amp;8&lt;id&gt; &amp;7领取奖励
 * &amp;7管理
 * &amp;8» &amp;f/ptxa reload &amp;7重载配置
 * &amp;8第 1/2 页 · &amp;f/ptx help 2 &amp;8查看更多
 * </pre>
 *
 * <p>用法（推荐用 {@link HelpProvider} 自动生成，无需手写清单）：</p>
 * <pre>{@code
 * CommandHelp.builder("MyPlugin")
 *         .entry("/mycmd", "主命令")
 *         .group("管理")
 *         .entry("/mycmd reload", "重载配置", "mycmd.admin")
 *         .send(sender);
 * }</pre>
 *
 * <p>本类只负责渲染，不依赖日志/消息服务，因此可安全用于测试。</p>
 *
 * @author yvmou
 * @since 1.0.0
 */
public final class CommandHelp {

    /** 标题前缀的删除线（纯结构字符，不属于文案）。 */
    private static final String RULE = "&8&m----------&r ";
    /** 分组标题颜色。 */
    private static final String GROUP_COLOR = "&7";
    /** 每行前缀记号。 */
    private static final String BULLET = "&8» &f";
    /** 紧凑模式下分行符的默认取值：一行放两条。 */
    private static final String DEFAULT_SEPARATOR = " &7| &f";
    /** 说明文字颜色。 */
    private static final String DESCRIPTION_COLOR = "&7";
    /** 权限提示颜色。 */
    private static final String PERMISSION_COLOR = "&8";

    /** 默认每页条数：聊天窗口约 10 行可见，留出标题与页脚的位置。 */
    public static final int DEFAULT_PAGE_SIZE = 8;

    private final String title;
    private final List<Entry> entries = new ArrayList<>();
    private String subtitle;
    private String usageLabel = "用法";
    private String footerHint = "查看更多";
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int page = 1;
    private String commandLabel;
    private int perLine = 1;
    private String separator = DEFAULT_SEPARATOR;

    private CommandHelp(@NotNull String title) {
        this.title = title;
    }

    /**
     * 创建帮助构建器。
     *
     * @param title 标题，通常是插件名
     */
    public static CommandHelp builder(@NotNull String title) {
        return new CommandHelp(title);
    }

    /**
     * 创建帮助构建器，并从带注解的命令类自动扫描条目。
     * <p>
     * 与 {@link #of(String, Object)} 的区别：只传 {@code Class}，
     * 因此<b>完全不需要实例化命令类</b>——静态上下文（如 {@code static} 的
     * 帮助方法）里也能安全使用。
     *
     * @param title        标题
     * @param commandClass 带 {@code @Command} 的类
     */
    public static CommandHelp ofAnnotations(@NotNull String title, @NotNull Class<?> commandClass) {
        CommandHelp help = new CommandHelp(title);
        help.entriesFrom(commandClass);
        return help;
    }

    /**
     * 创建帮助构建器，并自动从带注解的命令类扫描条目。
     *
     * @param title           标题
     * @param commandInstance 被 {@code @Command} 标注的实例（可为 null，此时只渲染标题）
     */
    public static CommandHelp of(@NotNull String title, @Nullable Object commandInstance) {
        CommandHelp help = new CommandHelp(title);
        if (commandInstance != null) {
            help.entriesFrom(commandInstance);
        }
        return help;
    }

    /** 设置标题下方的副标题（例如插件版本），可为 null。 */
    public CommandHelp subtitle(@Nullable String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /** 自定义「用法」这一分组标题的文案。 */
    public CommandHelp usageLabel(@NotNull String usageLabel) {
        this.usageLabel = usageLabel;
        return this;
    }

    /** 自定义页脚「查看更多」的文案。 */
    public CommandHelp footerHint(@NotNull String footerHint) {
        this.footerHint = footerHint;
        return this;
    }

    /**
     * 设置每行放几条命令，用于压缩行数（默认 1，即每条命令独占一行）。
     * <p>
     * 命令较多时可设为 2~3 减少刷屏；页脚的分页提示会随每页条数一起变化。
     *
     * @param perLine 每行条数，最小为 1
     */
    public CommandHelp perLine(int perLine) {
        this.perLine = Math.max(1, perLine);
        return this;
    }

    /** 自定义紧凑模式下的分行符（默认 {@code " | "}）。 */
    public CommandHelp separator(@NotNull String separator) {
        this.separator = separator;
        return this;
    }

    /** 设置每页条数，最小为 1。 */
    public CommandHelp pageSize(int pageSize) {
        // 每页条数会换算成「页大小」，需要按每行条数放大，否则一页装不下 perLine 条
        this.pageSize = Math.max(1, pageSize) * perLine;
        return this;
    }

    /**
     * 设置当前页码，从 1 开始。
     * <p>
     * 越界会被夹到有效范围内，因此调用方不必先算总页数。
     */
    public CommandHelp page(int page) {
        this.page = Math.max(1, page);
        return this;
    }

    /** 添加一条命令帮助。 */
    public CommandHelp entry(@NotNull String usage, @NotNull String description) {
        return entry(usage, description, null);
    }

    /** 添加一条命令帮助，并标注所需权限。 */
    public CommandHelp entry(@NotNull String usage, @NotNull String description, @Nullable String permission) {
        entries.add(new Entry(null, usage, description, permission));
        return this;
    }

    /**
     * 添加一个分组标题，后续 {@link #entry} 都归属该分组。
     *
     * @param name 分组名；传 null 表示回到无分组状态
     */
    public CommandHelp group(@Nullable String name) {
        entries.add(new Entry(name, null, null, null));
        return this;
    }

    /**
     * 从带注解的命令类自动扫描条目。
     * <p>
     * 帮助文案取自 {@code @Command(description)} 与 {@code @SubCommand(description)}，
     * 因此插件只要在注解里写清描述，帮助就是自动且一致的——
     * 不需要再维护一份手写清单（那种清单迟早会和代码不一致）。
     * <p>
     * 扫描不到的 {@code @SubCommand("")}（主命令自身）会渲染为只有命令名的形式。
     */
    public CommandHelp entriesFrom(@NotNull Object commandInstance) {
        return entriesFrom(commandInstance.getClass());
    }

    /** 从带注解的命令类扫描条目（不需要实例化）。 */
    public CommandHelp entriesFrom(@NotNull Class<?> commandClass) {
        // 扫描结果与本类的私有 Entry 是不同类型，这里做一次转换
        for (HelpProvider.Entry scanned : HelpProvider.scan(commandClass)) {
            if (scanned.isCommand()) {
                entries.add(Entry.command(scanned.usage(), scanned.description(), scanned.permission()));
            } else {
                entries.add(Entry.group(scanned.group()));
            }
        }
        if (commandLabel == null) {
            // 页脚提示要用真实命令名（如 "ptx help 2"），而不是写死的 "help"
            commandLabel = HelpProvider.commandName(commandClass);
        }
        return this;
    }

    /** 当前条目数（不含标题与页脚）。 */
    public int size() {
        return (int) entries.stream().filter(Entry::isCommand).count();
    }

    /** 按当前页大小计算的总页数，至少为 1。 */
    public int totalPages() {
        int commands = size();
        if (commands == 0) {
            return 1;
        }
        return (commands + pageSize - 1) / pageSize;
    }

    /**
     * 渲染为若干行（已带颜色码，可直接发给玩家）。
     * <p>
     * 与 {@link #send(CommandSender)} 共用同一套渲染逻辑，便于单元测试断言输出。
     */
    public List<String> lines() {
        List<String> lines = new ArrayList<String>();
        lines.add(color(RULE + "&6" + title + " " + RULE));

        if (subtitle != null && !subtitle.isEmpty()) {
            lines.add(color(GROUP_COLOR + subtitle));
        }

        int totalPages = totalPages();
        int currentPage = Math.min(page, totalPages);
        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, entries.size());

        // 分组标题不计入分页配额：把范围内的命令及其所属分组一起取出
        String pendingGroup = null;
        StringBuilder row = new StringBuilder();
        // 分组内单独分行：不同分组的命令不应挤在同一行
        int inRow = 0;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (!entry.isCommand()) {
                pendingGroup = entry.group();
                continue;
            }
            int commandIndex = commandIndexOf(i);
            if (commandIndex < from || commandIndex >= to) {
                continue;
            }
            if (pendingGroup != null) {
                if (row.length() > 0) {
                    lines.add(row.toString());
                    row.setLength(0);
                    inRow = 0;
                }
                lines.add(color(GROUP_COLOR + pendingGroup));
                pendingGroup = null;
            }
            if (inRow > 0) {
                row.append(color(separator));
            }
            row.append(renderEntry(entry));
            inRow++;
            if (inRow >= perLine) {
                lines.add(row.toString());
                row.setLength(0);
                inRow = 0;
            }
        }
        if (row.length() > 0) {
            lines.add(row.toString());
        }

        if (totalPages > 1) {
            lines.add(renderFooter(currentPage, totalPages));
        }
        return lines;
    }

    /** 把帮助发送给指定接收者。 */
    public void send(@NotNull CommandSender sender) {
        for (String line : lines()) {
            sender.sendMessage(line);
        }
    }

    /** 第 index 个条目之前（不含）有多少条命令，用于把条目下标换算成命令序号。 */
    private int commandIndexOf(int index) {
        int count = 0;
        for (int i = 0; i < index; i++) {
            if (entries.get(i).isCommand()) {
                count++;
            }
        }
        return count;
    }

    private String renderEntry(Entry entry) {
        StringBuilder builder = new StringBuilder(BULLET).append(entry.usage());
        if (entry.description() != null && !entry.description().isEmpty()) {
            builder.append(' ').append(DESCRIPTION_COLOR).append(entry.description());
        }
        if (entry.permission() != null && !entry.permission().isEmpty()) {
            builder.append(' ').append(PERMISSION_COLOR).append('[').append(entry.permission()).append(']');
        }
        return color(builder.toString());
    }

    private String renderFooter(int currentPage, int totalPages) {
        StringBuilder builder = new StringBuilder("&8第 ").append(currentPage).append('/').append(totalPages).append(" 页");
        if (currentPage < totalPages) {
            builder.append(" · &f");
            // commandLabel 是 @Command(name) 的裸名，提示里要带前导斜杠才是可复制的命令
            builder.append(commandLabel == null ? "/help" : "/" + commandLabel + " help");
            builder.append(' ').append(currentPage + 1);
            builder.append(" &8").append(footerHint);
        }
        return color(builder.toString());
    }

    /**
     * 把 {@code &} 颜色码转成 {@code §}。
     * <p>
     * 用 Bukkit 的 ChatColor 而不是自带转换：语义完全一致，且 spigot-api 是唯一依赖。
     */
    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * 一条帮助记录：分组标题（usage 为 null）或命令条目。
     * <p>
     * 用普通类而不是 record：YLib 的 api 模块以 Java 8 为目标，以兼容旧服务端。
     */
    private static final class Entry {

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

        String group() {
            return group;
        }

        String usage() {
            return usage;
        }

        String description() {
            return description;
        }

        String permission() {
            return permission;
        }

        boolean isCommand() {
            return usage != null;
        }
    }
}
