package cn.yvmou.ylib.gui.appearance;

import cn.yvmou.ylib.gui.MenuItem;
import cn.yvmou.ylib.gui.SlotLayout;
import cn.yvmou.ylib.text.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 外观的**校验、契约对账、代码构造与文件管理**。都是「关于外观本身」的事，收在一处：
 * <ul>
 *   <li>{@link #file(Plugin, String, MenuAppearance)} —— <b>代码定义默认值，文件交给 YLib 生成与读取</b>：
 *       文件不存在就按默认值生成（带注释），存在就读它——文件里写了的以文件为准，缺的键回落代码默认值；
 *       写坏的键只回退它自己并记警告，界面永远开得起来；</li>
 *   <li>{@link #reloadFiles()} / {@link #files()} —— 重载全部已登记文件 / 取出全部文件管理的外观；</li>
 *   <li>{@link #validate(MenuAppearance)} —— 校验：布局能解析、材质认得出来；</li>
 *   <li>{@link #contract(MenuAppearance)} —— 「这一格能用哪些占位符」的报告（配 {@code /yqa contract} 这类命令）；</li>
 *   <li>{@link #create(String)} —— 在代码里造一份外观：给「界面完全由代码决定、不要配置文件」的场合。</li>
 * </ul>
 * <p>
 * 对账的策略开关也在这里：{@link #checkContract()} / {@link #strictContract()} / {@link #logger(Consumer)}，
 * 宿主启动时设一次即可。
 */
public final class MenuAppearances {

    /** 是否做占位符对账（默认开：不一致会记日志，但不会打断界面）。 */
    private static volatile boolean checkContract = true;

    /** 对账发现「模板要的没给」时是抛还是记日志（默认记日志）。 */
    private static volatile boolean strictContract = false;

    /** 日志出口；宿主没接管时打到服务端控制台。 */
    private static volatile Consumer<String> logger;

    /** 每份外观最近一次构建的账本（键 = 逻辑菜单名），给报告用。 */
    private static final Map<String, Record> RECORDS = new ConcurrentHashMap<String, Record>();

    private MenuAppearances() {
    }

    // ---------- 开关 ----------

    /**
     * 是否做占位符对账。
     * <p>
     * 关掉只停掉「模板要的没给」的检查（不再记警告，{@link #strictContract()} 也不再生效）；
     * 账本**仍然记录**——{@link #contract(MenuAppearance)} 的运行侧（给了但没用上 / 配了但从没被摆过）
     * 正是靠它，关掉检查时反而更需要这份报告。
     */
    public static boolean checkContract() {
        return checkContract;
    }

    public static void checkContract(boolean enabled) {
        checkContract = enabled;
    }

    /**
     * 对账发现不一致时是否**直接抛**（默认 {@code false}：只记日志）。
     * <p>
     * 默认不抛是因为改配置的人可能是服主：他往 lore 里加一个自己编的 {@code {count}}，不该让整个菜单打不开。
     * 开发服 / CI 想要「错就炸」就打开它。
     */
    public static boolean strictContract() {
        return strictContract;
    }

    public static void strictContract(boolean strict) {
        strictContract = strict;
    }

    /** 接管日志出口（例如转给宿主自己的日志器）；不接管就打控制台。 */
    public static void logger(Consumer<String> sink) {
        logger = sink;
    }

    /** 记一条对账警告。 */
    public static void warn(String message) {
        Consumer<String> sink = logger;
        if (sink != null) {
            sink.accept(message);
            return;
        }
        Bukkit.getLogger().warning("[YLib] " + message);
    }

    // ---------- 账本 ----------

    /** 一份外观最近一次构建的记账：每个语言键「要哪些」与「给了哪些」占位符名字。 */
    public static final class Record {

        private final Map<String, Set<String>> required;
        private final Map<String, Set<String>> provided;
        private final int builds;

        Record(Map<String, Set<String>> required, Map<String, Set<String>> provided, int builds) {
            this.required = required;
            this.provided = provided;
            this.builds = builds;
        }

        /** 语言键 → 模板里要到的名字。它的键集合就是「这次摆过哪些配置项」。 */
        public Map<String, Set<String>> required() {
            return required;
        }

        /** 语言键 → 代码给出的名字。 */
        public Map<String, Set<String>> provided() {
            return provided;
        }

        /** 这份菜单被构建过几次。 */
        public int builds() {
            return builds;
        }

        /** 给了但当前配置与语言文件都没用到的名字（不影响显示，只是没人要）。 */
        public Set<String> unused() {
            Set<String> unused = new LinkedHashSet<String>();
            for (Map.Entry<String, Set<String>> entry : provided.entrySet()) {
                Set<String> wanted = required.get(entry.getKey());
                for (String name : entry.getValue()) {
                    if (wanted == null || !wanted.contains(name)) {
                        unused.add(name);
                    }
                }
            }
            return unused;
        }
    }

    /** 记下这次构建的账本（由 {@code Menu} 在构建末尾调用）。 */
    public static void record(MenuAppearance appearance, Map<String, Set<String>> required,
                              Map<String, Set<String>> provided) {
        if (appearance == null) {
            return;
        }
        String path = appearance.path();
        Record previous = RECORDS.get(path);
        int builds = previous == null ? 1 : previous.builds() + 1;
        RECORDS.put(path, new Record(copy(required), copy(provided), builds));
    }

    /** 这份外观最近一次的账本；从没构建过时为 {@code null}。 */
    public static Record record(String path) {
        return RECORDS.get(path);
    }

    private static Map<String, Set<String>> copy(Map<String, Set<String>> source) {
        Map<String, Set<String>> copy = new LinkedHashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashSet<String>(entry.getValue()));
        }
        return copy;
    }

    // ---------- 契约报告 ----------

    /**
     * 生成契约报告：**配置侧**（这份外观每一格、每一处文案能用哪些占位符）与**运行侧**
     * （如果这份菜单被打开过：给了什么没人用、配了什么从没被摆过）。
     * <p>
     * 配置侧只读外观就能算，因此永远都有；它是「某个占位符到底存不存在」的直接答案。
     * 纯文本、不带颜色码，方便直接复制去核对配置。
     */
    public static List<String> contract(MenuAppearance appearance) {
        List<String> lines = new ArrayList<String>();
        if (appearance == null) {
            return lines;
        }
        lines.add(appearance.fileName() + "（" + appearance.path() + "）");

        for (Map.Entry<String, ? extends MenuItemAppearance> entry : appearance.items().entrySet()) {
            MenuItemAppearance item = entry.getValue();
            Set<String> names = new LinkedHashSet<String>();
            if (item != null) {
                names.addAll(Placeholders.names(item.name()));
                names.addAll(Placeholders.names(item.lore()));
            }
            lines.add("  items." + entry.getKey() + " -> " + describe(names));
        }
        for (Map.Entry<String, List<String>> entry : appearance.texts().entrySet()) {
            lines.add("  " + entry.getKey() + " -> " + describe(Placeholders.names(entry.getValue())));
        }

        Record record = RECORDS.get(appearance.path());
        if (record == null) {
            lines.add("  （这份菜单还没被打开过：以上只有配置侧，运行侧要等它被打开一次）");
            return lines;
        }
        lines.add("  运行侧：打开过 " + record.builds() + " 次");
        Set<String> unused = record.unused();
        lines.add("    给了但当前配置没用上：" + describe(unused));
        Set<String> neverPlaced = new LinkedHashSet<String>();
        for (String name : appearance.items().keySet()) {
            if (!record.required().containsKey(appearance.itemKey(name))) {
                neverPlaced.add("items." + name);
            }
        }
        lines.add("    配了但从没被摆过：" + describe(neverPlaced));
        return lines;
    }

    private static String describe(Set<String> names) {
        return names == null || names.isEmpty() ? "（无）" : names.toString();
    }

    // ---------- 校验 ----------

    /** 校验一份外观：布局能解析、每个物品的材质认得出来。任何一条不合格都抛，消息里带文件名与键名。 */
    public static void validate(MenuAppearance appearance) {
        if (appearance == null) {
            throw new IllegalArgumentException("菜单外观是 null：没有外观就没有界面可开");
        }
        validateLayout(appearance);
        validateItems(appearance);
    }

    /** 校验布局表：至少要有一行，且每行不超过 9 格、行数放得进容器。 */
    public static void validateLayout(MenuAppearance appearance) {
        String[] layout = appearance.layout();
        if (layout == null || layout.length == 0) {
            throw new IllegalArgumentException(appearance.fileName() + " 的 layout 是空的（至少要有一行）");
        }
        try {
            SlotLayout.parse(SlotLayout.sizeOf(layout), layout);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(appearance.fileName() + " 的 layout 写错了: " + e.getMessage(), e);
        }
    }

    /** 校验物品表：材质名必须能被当前服务端认出来，否则界面上只会看到一张纸。 */
    public static void validateItems(MenuAppearance appearance) {
        for (Map.Entry<String, ? extends MenuItemAppearance> entry : appearance.items().entrySet()) {
            String where = appearance.fileName() + " 的 items." + entry.getKey();
            MenuItemAppearance item = entry.getValue();
            if (item == null) {
                throw new IllegalArgumentException(where + " 是空的：至少要写一个 material");
            }
            if (MenuItem.resolveMaterial(item.material()) == null) {
                throw new IllegalArgumentException(where + ".material 认不出来: "
                        + item.material() + "（写错的表现是图标变成纸）");
            }
        }
    }

    /** 解析材质名；空值与认不出来的名字都以 {@code null} 表示（与 {@link MenuItem#resolveMaterial} 同一套规则）。 */
    public static Material resolveMaterial(String name) {
        return MenuItem.resolveMaterial(name);
    }

    // ---------- 在代码里造外观 ----------

    /**
     * 在代码里造一份外观：界面完全由代码决定、没有任何东西要交给配置时用它。
     * <p>
     * 之所以不是「另一套手写菜单的机制」：外观是纯数据，谁来提供都一样——YAML 提供的走配置，
     * 代码提供的走这里，菜单侧只有一个 {@code Menu}，契约对账对两者同样生效。
     * <pre>{@code
     * MenuAppearance appearance = MenuAppearances.create("&e商店")
     *         .layout("#########", "#`buy`#####", "#########")
     *         .item("buy", "DIAMOND", "&b买一颗钻石", "&7价格 100")
     *         .build();
     * Menu.of(viewer, messages, appearance, null, menu -> menu.item("buy", click -> buy())).open();
     * }</pre>
     */
    public static Builder create(String title) {
        return new Builder(title);
    }

    /** {@link #create(String)} 的构造器。 */
    public static final class Builder {

        private final List<String> layout = new ArrayList<String>();
        private final Map<String, MenuItemAppearance> items = new LinkedHashMap<String, MenuItemAppearance>();
        private final Map<String, List<String>> texts = new LinkedHashMap<String, List<String>>();
        private final String title;
        private String path = "";

        Builder(String title) {
            this.title = title;
        }

        /** 逻辑菜单名（语言键前缀 {@code gui.<它>.…}）。不设就没有语言键，模板直接当文案用。 */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /** 布局表，一行一串字符（见 {@code SlotLayout}）。它会决定容器大小。 */
        public Builder layout(String... rows) {
            if (rows != null) {
                Collections.addAll(layout, rows);
            }
            return this;
        }

        /**
         * 一格物品：槽位名、材质、显示名、以及若干行 lore。
         * 显示名与 lore 里可以写 {@code {名字}}，由动作那边用 {@code Placeholders.vars(...)} 填。
         */
        public Builder item(String slot, String material, String name, String... lore) {
            items.put(slot, new BuiltItem(material, name, lore));
            return this;
        }

        /** 一处数据驱动的文案（列表条目的 lore 这类），后缀与 {@code textOf}/{@code loreOf} 对应。 */
        public Builder text(String suffix, String... lines) {
            List<String> templates = new ArrayList<String>();
            if (lines != null) {
                Collections.addAll(templates, lines);
            }
            texts.put(suffix, templates);
            return this;
        }

        public MenuAppearance build() {
            return new Built(path, title, layout.toArray(new String[layout.size()]), items, texts);
        }
    }

    /** 代码造出来的外观（不可变值）。 */
    private static final class Built implements MenuAppearance {

        private final String path;
        private final String title;
        private final String[] layout;
        private final Map<String, MenuItemAppearance> items;
        private final Map<String, List<String>> texts;

        Built(String path, String title, String[] layout,
              Map<String, MenuItemAppearance> items, Map<String, List<String>> texts) {
            this.path = path;
            this.title = title;
            this.layout = layout;
            this.items = items;
            this.texts = texts;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String[] layout() {
            return layout;
        }

        @Override
        public Map<String, ? extends MenuItemAppearance> items() {
            return items;
        }

        @Override
        public Map<String, List<String>> texts() {
            return texts;
        }
    }

    /** 代码造出来的物品外观。 */
    private static final class BuiltItem implements MenuItemAppearance {

        private final String material;
        private final String name;
        private final List<String> lore;

        BuiltItem(String material, String name, String[] lore) {
            this.material = material;
            this.name = name;
            List<String> lines = new ArrayList<String>();
            if (lore != null) {
                Collections.addAll(lines, lore);
            }
            this.lore = lines;
        }

        @Override
        public String material() {
            return material;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> lore() {
            return lore;
        }
    }

    // ---------- 文件：代码定义默认值，YLib 生成与读取 ----------

    /**
     * 已登记的外观文件（文件绝对路径 → 状态）。注册顺序保留——报告按登记顺序列。
     * 登记与重载都发生在主线程（启动 / 命令），不需要并发结构。
     */
    private static final Map<String, Managed> FILES = new LinkedHashMap<String, Managed>();

    /**
     * 把一份代码里定义的**默认外观**交给一个配置文件管理，返回这份菜单的外观。
     * <p>
     * <b>这是「菜单外观可配」的唯一写法</b>：宿主不再为每个菜单写一个配置类，默认外观（标题 / 布局 /
     * 物品 / 文案模板）写在菜单自己的代码里，YLib 负责文件那半边：
     * <ul>
     *   <li>文件不存在 → 按默认值生成（带注释），生成的就是一份能直接改的配置；</li>
     *   <li>文件存在 → 读它：<b>文件里写了的以文件为准，缺的键回落代码默认值</b>（升级友好：插件新增
     *       一个按钮 / 一行文案，老配置文件不用动）；</li>
     *   <li>写坏了 → 只回退坏的那一项并记警告（布局坏 → 默认布局；某个物品的材质坏 → 那一格用默认），
     *       界面永远开得起来——一个键打错不至于让整个菜单的自定义全丢；</li>
     *   <li>重复登记同一文件 → 按第一次算（同一文件只有一份默认值来源）。</li>
     * </ul>
     * 两类错误<b>当场抛</b>（它们是代码 bug，不是配置问题）：默认值本身不合法（{@link #validate}），
     * 或默认值没写 {@code path(...)}（它是语言键前缀，文件管理的外观必须有）。
     * <p>
     * 返回的是**活的外观**：菜单拿它构造即可；{@link #reloadFiles()} 之后，**新建**的菜单看到新值。
     * 已经建好的那个实例只能改内容——容器大小在构造时就定稿了，**改了布局行数要重开菜单**，
     * 否则 {@code refresh()} 会抛「布局表写了 N 行，容器大小 M 只放得下 K 行」。
     */
    public static MenuAppearance file(Plugin plugin, String fileName, MenuAppearance defaults) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(fileName, "fileName");
        if (defaults == null) {
            throw new IllegalArgumentException(fileName + " 的默认外观是 null：没有默认值就没有可生成的文件");
        }
        validate(defaults);
        if (defaults.path() == null || defaults.path().isEmpty()) {
            throw new IllegalArgumentException(fileName + " 的默认外观没写 path(...)：它是这份菜单的语言键前缀"
                    + "（gui.<它>.…），文件管理的外观必须有");
        }
        String name = normalizeFileName(fileName);
        String key = new File(plugin.getDataFolder(), name).getAbsolutePath();
        Managed managed = FILES.get(key);
        if (managed != null) {
            return managed.view;
        }
        managed = new Managed(plugin, name, defaults);
        FILES.put(key, managed);
        managed.view.current = managed.load();
        return managed.view;
    }

    /** 重读全部已登记的外观文件（{@code /xxx reload config} 这类入口调它）。逐份独立：一份坏了只回退它那一份并记警告。 */
    public static void reloadFiles() {
        for (Managed managed : FILES.values()) {
            managed.view.current = managed.load();
        }
    }

    /** 全部文件管理的外观，按登记顺序；校验与契约报告的清单从这里取。 */
    public static List<MenuAppearance> files() {
        List<MenuAppearance> appearances = new ArrayList<MenuAppearance>(FILES.size());
        for (Managed managed : FILES.values()) {
            appearances.add(managed.view);
        }
        return appearances;
    }

    private static String normalizeFileName(String fileName) {
        String name = fileName.startsWith("/") || fileName.startsWith("\\") ? fileName.substring(1) : fileName;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml") ? name : name + ".yml";
    }

    /** 一份被管理的文件：默认值 + 当前快照；{@link #load()} 负责生成 / 读取 / 合并 / 回退。 */
    private static final class Managed {

        private final Plugin plugin;
        private final String fileName;
        private final MenuAppearance defaults;
        private final FileAppearance view;

        Managed(Plugin plugin, String fileName, MenuAppearance defaults) {
            this.plugin = plugin;
            this.fileName = fileName;
            this.defaults = defaults;
            this.view = new FileAppearance(fileName, defaults.path(), defaults);
        }

        /**
         * 生成（缺文件时）并读取、与默认值合并；坏的键回退默认并逐条记警告。
         * 无论哪一步失败都返回一个可用快照（最坏情况就是默认值本身），因此调用方永远拿到能用的外观。
         */
        private MenuAppearance load() {
            File file = new File(plugin.getDataFolder(), fileName);
            List<String> problems = new ArrayList<String>();
            if (!file.isFile()) {
                try {
                    writeDefaults(file);
                } catch (Exception e) {
                    problems.add("按默认值生成不了（" + e.getMessage() + "），先用代码里的默认外观");
                }
            }
            MenuAppearance snapshot;
            if (file.isFile()) {
                YamlConfiguration yaml = new YamlConfiguration();
                try {
                    yaml.load(file);
                    snapshot = merge(yaml, problems);
                } catch (Exception e) {
                    problems.add("读不出来（" + e.getMessage() + "），先用代码里的默认外观");
                    snapshot = defaults;
                }
            } else {
                snapshot = defaults;
            }
            for (String problem : problems) {
                warn(fileName + " " + problem);
            }
            return snapshot;
        }

        /** 文件 → 外观：文件里写了的以文件为准，缺的 / 坏的回落默认值。 */
        private MenuAppearance merge(YamlConfiguration yaml, List<String> problems) {
            String title = textOrDefault(yaml.getString("title"), defaults.title());

            String[] layout = defaults.layout();
            if (yaml.isList("layout")) {
                List<String> rows = yaml.getStringList("layout");
                if (rows.isEmpty()) {
                    problems.add("的 layout 是空的（至少要有一行），先用默认布局");
                } else {
                    try {
                        String[] rowsArray = rows.toArray(new String[rows.size()]);
                        SlotLayout.parse(SlotLayout.sizeOf(rowsArray), rowsArray);
                        layout = rowsArray;
                    } catch (IllegalArgumentException e) {
                        problems.add("的 layout 写错了: " + e.getMessage() + "，先用默认布局");
                    }
                }
            }

            Map<String, MenuItemAppearance> items = new LinkedHashMap<String, MenuItemAppearance>();
            ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
            for (Map.Entry<String, ? extends MenuItemAppearance> entry : defaults.items().entrySet()) {
                items.put(entry.getKey(), mergeItem(itemsSection, entry.getKey(), entry.getValue(), problems));
            }
            if (itemsSection != null) {
                for (String name : itemsSection.getKeys(false)) {
                    if (items.containsKey(name)) {
                        continue;
                    }
                    MenuItemAppearance fileOnly = mergeItem(itemsSection, name, null, problems);
                    if (fileOnly != null) {
                        items.put(name, fileOnly);
                    }
                }
            }

            Map<String, List<String>> texts = new LinkedHashMap<String, List<String>>();
            for (Map.Entry<String, List<String>> entry : defaults.texts().entrySet()) {
                texts.put(entry.getKey(), readText(yaml, entry.getKey(), entry.getValue()));
            }

            Set<String> known = new LinkedHashSet<String>(
                    Arrays.asList("title", "layout", "items", "config-version"));
            known.addAll(defaults.texts().keySet());
            for (String key : yaml.getKeys(false)) {
                if (!known.contains(key)) {
                    problems.add("里有不认识的顶层键 " + key + "（不生效；可能是旧版本留下的）");
                }
            }
            return new Snapshot(fileName, defaults.path(), title, layout, items, texts);
        }

        /** 合并一格物品：文件里没写这一条 → 用默认；材质认不出来 → 那一格回退默认材质（文件独有的条目则整条不生效）。 */
        private MenuItemAppearance mergeItem(ConfigurationSection itemsSection, String name,
                                             MenuItemAppearance fallback, List<String> problems) {
            ConfigurationSection section = itemsSection == null ? null : itemsSection.getConfigurationSection(name);
            if (section == null) {
                return fallback;
            }
            String where = "的 items." + name;
            String material = section.isString("material")
                    ? section.getString("material")
                    : (fallback == null ? null : fallback.material());
            if (MenuItem.resolveMaterial(material) == null) {
                problems.add(where + ".material 认不出来: " + material
                        + (fallback == null ? "，这条先不生效" : "，先用默认材质"));
                material = fallback == null ? null : fallback.material();
                if (material == null) {
                    return null;
                }
            }
            String itemName = section.isString("name")
                    ? section.getString("name")
                    : (fallback == null ? "" : fallback.name());
            List<String> lore = section.isList("lore")
                    ? section.getStringList("lore")
                    : (fallback == null ? Collections.<String>emptyList() : fallback.lore());
            return new BuiltItem(material, itemName, lore.toArray(new String[lore.size()]));
        }

        /** 读一处数据文案：单行写成标量、多行写成列表，两种形态都认。 */
        private static List<String> readText(YamlConfiguration yaml, String key, List<String> fallback) {
            if (yaml.isString(key)) {
                return Collections.singletonList(yaml.getString(key));
            }
            if (yaml.isList(key)) {
                return yaml.getStringList(key);
            }
            return fallback;
        }

        private static String textOrDefault(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value;
        }

        /** 缺文件时按默认值生成一份带注释的配置。 */
        private void writeDefaults(File file) throws Exception {
            YamlConfiguration out = new YamlConfiguration();
            out.set("title", defaults.title());
            out.set("layout", Arrays.asList(defaults.layout()));
            for (Map.Entry<String, ? extends MenuItemAppearance> entry : defaults.items().entrySet()) {
                String base = "items." + entry.getKey();
                out.set(base + ".material", entry.getValue().material());
                out.set(base + ".name", entry.getValue().name());
                out.set(base + ".lore", entry.getValue().lore());
            }
            for (Map.Entry<String, List<String>> entry : defaults.texts().entrySet()) {
                out.set(entry.getKey(), entry.getValue().size() == 1
                        ? entry.getValue().get(0)
                        : entry.getValue());
                setComments(out, entry.getKey(), "数据文案模板：{名字} 占位符由代码填值");
            }
            setComments(out, "title", "菜单标题。直接写整串（& 码 / MiniMessage 都行）");
            setComments(out, "layout", "布局表：一行一项、每行最多 9 格、一个字符一格；`名字` 是按钮格，空格是留空");
            setComments(out, "items", "物品外观：键 = 布局里的按钮名，filler 是背景板。"
                    + "\nmaterial 是原版材质名（大小写不敏感）；name / lore 里可写 {名字} 占位符，由代码填值");
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IllegalStateException("创建目录失败: " + parent);
            }
            out.save(file);
        }

        /** 给生成文件里的键加注释；老服务端没有 setComments 就只是没注释，不影响读写。 */
        private static void setComments(YamlConfiguration config, String path, String comment) {
            try {
                Method method = config.getClass().getMethod("setComments", String.class, List.class);
                method.invoke(config, path, Arrays.asList(comment.split("\n")));
            } catch (ReflectiveOperationException e) {
                // 老服务端：注释是锦上添花
            }
        }
    }

    /** 文件管理的外观：身份（path / fileName）固定，内容指向当前快照——reloadFiles 之后读到的就是新值。 */
    private static final class FileAppearance implements MenuAppearance {

        private final String fileName;
        private final String path;
        private volatile MenuAppearance current;

        FileAppearance(String fileName, String path, MenuAppearance initial) {
            this.fileName = fileName;
            this.path = path;
            this.current = initial;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String title() {
            return current.title();
        }

        @Override
        public String[] layout() {
            return current.layout();
        }

        @Override
        public Map<String, ? extends MenuItemAppearance> items() {
            return current.items();
        }

        @Override
        public Map<String, List<String>> texts() {
            return current.texts();
        }

        @Override
        public String fileName() {
            return fileName;
        }
    }

    /** 一次加载的合并结果（不可变值）。 */
    private static final class Snapshot implements MenuAppearance {

        private final String fileName;
        private final String path;
        private final String title;
        private final String[] layout;
        private final Map<String, MenuItemAppearance> items;
        private final Map<String, List<String>> texts;

        Snapshot(String fileName, String path, String title, String[] layout,
                 Map<String, MenuItemAppearance> items, Map<String, List<String>> texts) {
            this.fileName = fileName;
            this.path = path;
            this.title = title;
            this.layout = layout;
            this.items = items;
            this.texts = texts;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String[] layout() {
            return layout;
        }

        @Override
        public Map<String, MenuItemAppearance> items() {
            return items;
        }

        @Override
        public Map<String, List<String>> texts() {
            return texts;
        }

        @Override
        public String fileName() {
            return fileName;
        }
    }
}
