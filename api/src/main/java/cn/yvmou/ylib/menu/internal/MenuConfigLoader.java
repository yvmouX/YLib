package cn.yvmou.ylib.menu.internal;

import cn.yvmou.ylib.menu.ActionKind;
import cn.yvmou.ylib.menu.Items;
import cn.yvmou.ylib.menu.MenuConfig;
import cn.yvmou.ylib.menu.MenuConfigException;
import cn.yvmou.ylib.menu.MenuDefinition;
import cn.yvmou.ylib.menu.MenuTemplate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 配置文件层：代码给默认值，文件负责覆盖。
 *
 * <h2>覆盖规则（写死，不留含糊）</h2>
 * <b>配置优先于代码，覆盖粒度是「符号」，不是「槽位」。</b>
 * <table>
 *   <caption>合并语义</caption>
 *   <tr><th>情况</th><th>行为</th></tr>
 *   <tr><td>yml 缺某个符号</td><td>用代码默认值</td></tr>
 *   <tr><td>yml 缺某个字段（只写了 name）</td><td>该字段用代码默认值，其余字段合并</td></tr>
 *   <tr><td>yml 有布局里不存在的符号</td><td>忽略 + 启动 warning</td></tr>
 *   <tr><td>布局里有符号但 yml 没有</td><td>用代码默认值，不报错</td></tr>
 *   <tr><td>yml {@code version} &lt; 代码 {@code version}</td><td>跑迁移链，升版后<b>备份原文件</b>再写回</td></tr>
 *   <tr><td>yml {@code version} &gt; 代码 {@code version}</td><td><b>拒绝加载该文件</b>，提示升级插件</td></tr>
 *   <tr><td>yml 语法错误</td><td>启动失败并打印原因（<b>不静默降级</b>）</td></tr>
 * </table>
 *
 * <h2>「配置脏数据」与「代码写错」两种错误分开处理</h2>
 * 材质名认不出来、符号不在布局里这类属于**配置**问题：记警告、那一项回退默认值，界面照常能开
 * （一个键打错不该让整个菜单的自定义全丢）。而布局解析不了、perPage 与槽位数不匹配属于**代码**
 * 问题，在注册期就已经抛了。
 */
public final class MenuConfigLoader {

    /** 打进 jar 的只读内置配置前缀。 */
    private static final String RES_PREFIX = "res://";

    /**
     * 三种物品字段的说明文案。
     * <p>
     * 提成常量是刻意的：生成骨架与「给老文件补新键」都要写这三行，
     * 分成两处字面量迟早会漂移成两种说法。
     */
    private static final String COMMENT_MATERIAL =
            "原版材质名（大小写不敏感）；认不出来会回退代码默认材质";
    private static final String COMMENT_NAME = "显示名，支持 & 色码与 {占位符}";
    private static final String COMMENT_LORE = "每行一条，支持 & 色码与 {占位符}";

    private MenuConfigLoader() {
    }

    /**
     * 按 {@code @MenuConfig} 读取并叠加配置文件。符号的物品定义会被**就地替换**成合并后的结果。
     *
     * @param plugin     宿主插件（定位 dataFolder、读 jar 内资源）
     * @param template   模板实例（迁移时可能要调用它身上的方法）
     * @param definition 已经解析好的定义
     * @param config     模板类上的注解
     */
    public static void apply(Plugin plugin, MenuTemplate template, MenuDefinition definition,
                             MenuConfig config) {
        String path = config.path() == null ? "" : config.path().trim();
        if (path.isEmpty()) {
            throw new MenuConfigException(template.getClass().getName()
                    + " 的 @MenuConfig.path 是空的：不要配置文件就别标 @MenuConfig");
        }

        boolean fromJar = path.startsWith(RES_PREFIX);
        YamlConfiguration yaml;
        if (fromJar) {
            yaml = readFromJar(plugin, path.substring(RES_PREFIX.length()));
        } else {
            yaml = readFromDisk(plugin, template, definition, path, config.generateDefault(), config.version());
        }
        if (yaml == null) {
            return;      // 没有可读的文件：全部用代码默认值
        }

        int codeVersion = config.version();
        int fileVersion = yaml.getInt("version", codeVersion);

        if (fileVersion > codeVersion) {
            throw new MenuConfigException(path + " 的 version=" + fileVersion
                    + " 比代码的 version=" + codeVersion + " 新：请升级插件（或把 version 改回 "
                    + codeVersion + "）");
        }
        if (fileVersion < codeVersion) {
            migrate(plugin, template, path, yaml, config, fileVersion);
            // 迁移完之后按新版本重新读一次，保证「迁移产物」与「后续读取」看到的是同一份
            yaml = fromJar ? readFromJar(plugin, path.substring(RES_PREFIX.length()))
                    : readFromDisk(plugin, template, definition, path, false, config.version());
            if (yaml == null) {
                return;
            }
        }

        mergeItems(definition, path, yaml);
        // 升级友好：把布局里有、文件里还没有的符号键补进去（老配置文件因此不必手动改）
        fillMissingItems(plugin, definition, path, yaml, config.generateDefault());
        rejectUnknownTopLevelKeys(path, yaml);
    }

    // ---------- 读文件 ----------

    /** 读磁盘上的配置；{@code generateDefault} 时缺文件会先生成一份默认骨架。 */
    private static YamlConfiguration readFromDisk(Plugin plugin, MenuTemplate template,
                                                  MenuDefinition definition, String path,
                                                  boolean generateDefault, int version) {
        File file = fileOf(plugin, path);
        if (!file.isFile()) {
            if (!generateDefault) {
                return null;
            }
            writeDefaults(template, definition, path, file, version);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception failure) {
            // 语法错误一律抛：静默降级会让「我明明改了配置」变成一场排查噩梦
            throw new MenuConfigException(path + " 读不出来: " + failure.getMessage(), failure);
        }
        return yaml;
    }

    /** 读 jar 内的只读内置配置；没有就返回 {@code null}（全部用代码默认值）。 */
    private static YamlConfiguration readFromJar(Plugin plugin, String resource) {
        if (plugin == null) {
            return null;
        }
        InputStream stream = plugin.getResource(resource);
        if (stream == null) {
            return null;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception failure) {
            throw new MenuConfigException("jar 内置配置 " + resource + " 读不出来: "
                    + failure.getMessage(), failure);
        } finally {
            close(stream);
        }
        return yaml;
    }

    private static void close(InputStream stream) {
        try {
            stream.close();
        } catch (Exception ignored) {
            // 关不上就算了，这里不是致命路径
        }
    }

    // ---------- 生成默认骨架 ----------

    /**
     * 按代码默认值生成一份带注释的配置。
     * <p>
     * 生成出来的是**能直接改的文件**：每个符号都写上当前代码默认值，服主要改哪里一目了然。
     * 布局只是文件头的参考说明（改布局要动代码——容器的行数与槽位顺序由它决定，
     * 允许配置改布局就等于允许配置把 perPage 与槽位数搞不一致）。
     */
    private static void writeDefaults(MenuTemplate template, MenuDefinition definition,
                                      String path, File file, int version) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("version", Integer.valueOf(version));

        for (SymbolState state : definition.symbols().values()) {
            if (state.isPaged() || state.items() == null || state.items().rawStack() != null) {
                // 分页符号与成品物品没有可覆盖的外观，写出来只会让人以为改了有用
                continue;
            }
            // 与「给老文件补键」共用同一段写入逻辑，两处的字段与注释因此不可能漂移
            writeItem(yaml, "items." + state.configKey(), state.items());
        }

        // 布局说明并进 version 的注释里，而不是单独发在 "layout" 这个键上：
        // 那个键不存在于文件里，注释会被静默丢掉（setComments 只对已存在的路径生效）。
        comment(yaml, "version", "配置版本号：比代码的 @MenuConfig.version 旧会跑迁移，新则拒绝加载\n"
                + "\n" + layoutSummary(template));
        comment(yaml, "items", "物品外观：键 = 代码里 item(\"...\") 写的那个键；"
                + "写了的以本文件为准，没写的用代码默认值\n"
                + "可选的 actions 会完全接管该格的点击行为（写了它，代码里的 onClick 被忽略）");

        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new MenuConfigException("创建目录失败: " + parent);
        }
        try {
            yaml.save(file);
        } catch (Exception failure) {
            throw new MenuConfigException("生成默认配置 " + path + " 失败: " + failure.getMessage(), failure);
        }
    }

    /** 文件头里的布局参考：把模板的布局原文抄一遍，省得服主为了看懂槽位去翻代码。 */
    private static String layoutSummary(MenuTemplate template) {
        StringBuilder text = new StringBuilder("布局由模板的 layout() 决定（改布局要动代码）。当前是：");
        String[] rows = template.layout();
        for (int index = 0; index < rows.length; index++) {
            text.append("\n  ").append(rows[index]);
        }
        return text.toString();
    }

    /**
     * 给生成文件里的键加注释。
     * <p>
     * 两个 Bukkit 怪癖都在这里兜住（与 {@code core:config} 的 {@code ConfigurationLoader} 一致）：
     * <ul>
     *   <li>{@code setComments} 只对**已存在**的路径生效——所以本方法必须在 {@code yaml.set(...)}
     *       之后调用，且不能给「文件里根本不存在的键」加注释（那会静默失效）；</li>
     *   <li>注释里的 {@code \r} 要裁掉：{@code \r\n} 的 {@code \r} 会留在行尾，YAML 里是控制字符，
     *       不报错但查不出来。</li>
     * </ul>
     * {@code setComments} 是较新版本才有的（老服务端上没有），因此走反射并在失败时静默跳过——
     * 注释是锦上添花，缺了不影响读写。
     */
    private static void comment(YamlConfiguration yaml, String key, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        List<String> lines = new ArrayList<String>();
        for (String line : text.split("\n")) {
            // 只用 \n 分段：\r\n 的 \r 会留在行尾
            lines.add(line.endsWith("\r") ? line.substring(0, line.length() - 1) : line);
        }
        try {
            Method method = yaml.getClass().getMethod("setComments", String.class, List.class);
            method.invoke(yaml, key, lines);
        } catch (ReflectiveOperationException ignored) {
            // 老服务端：没有注释也不影响读写
        } catch (RuntimeException ignored) {
            // 某些实现会对未知键抛：同样不影响读写
        }
    }

    // ---------- 合并 ----------

    /** 把 yml 里的 items 叠加到定义上：写了的以文件为准，缺的回落代码默认值。 */
    private static void mergeItems(MenuDefinition definition, String path, YamlConfiguration yaml) {
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        Set<String> seen = new LinkedHashSet<String>();
        for (String key : items.getKeys(false)) {
            seen.add(key);
            SymbolState state = definition.stateByConfigKey(key);
            if (state == null) {
                MenuLog.warn("配置项 items." + key + " 未在布局中使用，已忽略（" + path + "）");
                continue;
            }
            if (state.isPaged()) {
                MenuLog.warn("items." + key + " 是分页符号：条目由数据决定，"
                        + "配置里的 material / name / lore 不生效（" + path + "）");
                continue;
            }
            ConfigurationSection section = items.getConfigurationSection(key);
            if (section == null) {
                MenuLog.warn("items." + key + " 不是一段配置（缺 material / name / lore），已忽略（"
                        + path + "）");
                continue;
            }
            applyItem(definition, state, section, path, key);
            bindActions(state, section, path, key);
        }
        // 布局里没配的符号：用代码默认值，不报错（这是最正常的情况）
    }

    /**
     * 升级友好：把**代码里有、文件里还没有**的符号键补进文件。
     *
     * <h2>为什么值得写盘</h2>
     * 「缺的键回落代码默认值」保证了**运行**没问题，但服主打开文件看不到那一项——
     * 于是「这个菜单支持改哪些东西」只能靠翻文档。插件升级新增一个按钮时尤其明显：
     * 老配置文件里根本没有它的影子。
     *
     * <h2>三条自我约束</h2>
     * <ul>
     *   <li><b>只加不改</b>：文件里已有的键一律不碰——用户改过的值、用户写的注释全都留着；</li>
     *   <li><b>没变化就不写盘</b>：文件已完整时直接返回，免得每次启动 mtime 都变、
     *       让服主以为配置被动过；</li>
     *   <li><b>只在 {@code generateDefault = true} 时补</b>：那个开关的语义是「这个文件由 YLib 管」，
     *       关掉它表示文件由运维 / 版本控制提供，库不该往里写东西。</li>
     * </ul>
     *
     * @param generateDefault 来自 {@code @MenuConfig}；为 {@code false} 时只看不写
     */
    private static void fillMissingItems(Plugin plugin, MenuDefinition definition, String path,
                                         YamlConfiguration yaml, boolean generateDefault) {
        if (path.startsWith(RES_PREFIX)) {
            return;      // jar 内置只读配置：不落地、也不回写
        }
        boolean modified = false;
        int missing = 0;
        for (SymbolState state : definition.symbols().values()) {
            if (state.isPaged() || state.items() == null || state.items().rawStack() != null) {
                // 分页符号与成品物品没有可覆盖的外观（与生成骨架时的口径一致）
                continue;
            }
            if (fillMissingItem(yaml, state)) {
                modified = true;
                missing++;
            }
        }
        if (!modified) {
            return;
        }
        if (!generateDefault) {
            MenuLog.warn(path + " 有 " + missing + " 个符号的配置项缺失，但 "
                    + "@MenuConfig(generateDefault = false)：本库不替你写这个文件");
            return;
        }
        File file = fileOf(plugin, path);
        try {
            yaml.save(file);
            MenuLog.warn(path + " 补齐了 " + missing + " 个新版本的配置项（已有的值与注释都没动）");
        } catch (Exception failure) {
            // 写不进去不算致命：运行期用的是代码默认值，界面照常
            MenuLog.warn(path + " 补齐配置项失败: " + failure.getMessage());
        }
    }

    /** 补一个符号缺失的键；返回是否真的动了文件。 */
    private static boolean fillMissingItem(YamlConfiguration yaml, SymbolState state) {
        String base = "items." + state.configKey();
        Items items = state.items();

        // 整项缺失：连键带注释一起写进去，服主一眼就知道这一项是什么
        if (!yaml.contains(base)) {
            writeItem(yaml, base, items);
            return true;
        }

        // 项在、个别字段缺：只补缺的那些（用户可能是故意删掉 lore 的）
        boolean modified = false;
        if (!yaml.contains(base + ".material") && items.material() != null) {
            yaml.set(base + ".material", items.material().name());
            comment(yaml, base + ".material", COMMENT_MATERIAL);
            modified = true;
        }
        if (!yaml.contains(base + ".name")) {
            yaml.set(base + ".name", items.name());
            comment(yaml, base + ".name", COMMENT_NAME);
            modified = true;
        }
        if (!yaml.contains(base + ".lore")) {
            yaml.set(base + ".lore", loreOf(items));
            comment(yaml, base + ".lore", COMMENT_LORE);
            modified = true;
        }
        return modified;
    }

    /** 把一个符号的默认外观写进 yaml（含注释）；生成骨架与补键共用。 */
    private static void writeItem(YamlConfiguration yaml, String base, Items items) {
        yaml.set(base + ".material", items.material() == null ? "" : items.material().name());
        comment(yaml, base + ".material", COMMENT_MATERIAL);
        yaml.set(base + ".name", items.name());
        comment(yaml, base + ".name", COMMENT_NAME);
        yaml.set(base + ".lore", loreOf(items));
        comment(yaml, base + ".lore", COMMENT_LORE);
    }

    /** 空 lore 写成空列表（写入 {@code null} 会让 YAML 里整个键消失）。 */
    private static List<String> loreOf(Items items) {
        return items.lore() == null ? Collections.<String>emptyList()
                : new ArrayList<String>(items.lore());
    }

    /** 合并一格物品：逐个字段「文件里有就用文件，没有就回落代码默认值」。 */
    private static void applyItem(MenuDefinition definition, SymbolState state,
                                  ConfigurationSection section, String path, String key) {
        Items base = state.items();
        if (base == null) {
            MenuLog.warn("items." + key + " 对应的符号没有代码默认物品，无法合并（" + path + "）");
            return;
        }
        if (base.rawStack() != null) {
            MenuLog.warn("items." + key + " 对应的符号是 of(ItemStack) 直接接管的成品物品，"
                    + "配置覆盖不生效（" + path + "）");
            return;
        }

        // 从**代码默认值那一份**起步（不是只取它的材质）：这样文件没写的字段天然回落到默认值
        Items.Builder builder = Items.basedOn(base);
        if (section.isString("material")) {
            String material = section.getString("material");
            Material resolved = Items.resolve(material);
            // 认不出来 → 用代码默认材质并警告，而不是整条不生效：一个名字打错不该丢掉整格的文案
            if (resolved == null) {
                MenuLog.warn("items." + key + ".material 认不出来: " + material + Items.suggest(material)
                        + "，先用代码默认材质（" + path + "）");
            } else {
                // 只把材质这一项标成「文件给的」；名字 / lore 仍走同一套回落规则
                builder = Items.of(resolved);
            }
        }
        if (section.isString("name")) {
            builder.name(section.getString("name"));
        }
        if (section.isList("lore")) {
            List<String> lore = section.getStringList("lore");
            builder.lore(lore.toArray(new String[lore.size()]));
        }
        state.overrideItems(builder.resolve(base));
    }

    /**
     * 绑定 yml 的 {@code actions}：一旦配了，它**完全接管**这一格的点击行为。
     * <p>
     * 代码里的 {@code .onClick(...)} 被忽略，并在启动日志里提示一句——否则同一格上跑着两套逻辑，
     * 表现是「点了做了两件事」或者「改了配置却没反应」，两种都很难查。
     */
    private static void bindActions(SymbolState state, ConfigurationSection section,
                                    String path, String key) {
        if (!section.isList("actions")) {
            return;
        }
        List<ActionSpec> specs = parseActions(section, path, key);
        if (specs.isEmpty()) {
            return;
        }
        if (state.hasClick()) {
            MenuLog.warn("items." + key + " 配了 actions，代码里的 onClick 被忽略（" + path + "）");
        }
        state.actionClick(ActionBinder.bind(specs, MenuActionsHolder.get()));
    }

    /** 解析 {@code actions} 列表；单条坏掉只跳过那一条并记警告，不让整个插件启动失败。 */
    private static List<ActionSpec> parseActions(ConfigurationSection section, String path, String key) {
        List<ActionSpec> specs = new ArrayList<ActionSpec>();
        List<?> raw = section.getList("actions");
        if (raw == null) {
            return specs;
        }
        for (Object element : raw) {
            if (!(element instanceof java.util.Map)) {
                MenuLog.warn("items." + key + ".actions 里有一项不是「type: ...」这种写法，已跳过（"
                        + path + "）");
                continue;
            }
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) element;
            Object type = map.get("type");
            ActionKind kind = ActionKind.parse(type == null ? null : String.valueOf(type));
            if (kind == null) {
                MenuLog.warn("items." + key + ".actions 里有认不出来的 type: " + type
                        + "（可用：" + ActionKind.describeAll() + "）（" + path + "）");
                continue;
            }
            String parameter = kind.parameter();
            String argument = parameter == null ? null : string(map.get(parameter));
            if (parameter != null && (argument == null || argument.isEmpty())) {
                MenuLog.warn("items." + key + ".actions 的 " + kind.name().toLowerCase(java.util.Locale.ROOT)
                        + " 缺参数 " + parameter + "，已跳过（" + path + "）");
                continue;
            }
            specs.add(new ActionSpec(kind, argument, 0));
        }
        return specs;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 认不认识的顶层键：记一条警告（可能是旧版本留下的），不影响其余部分。 */
    private static void rejectUnknownTopLevelKeys(String path, YamlConfiguration yaml) {
        Set<String> known = new LinkedHashSet<String>(Arrays.asList("version", "items"));
        for (String key : yaml.getKeys(false)) {
            if (!known.contains(key)) {
                MenuLog.warn(path + " 里有不认识的顶层键 " + key + "（不生效；可能是旧版本留下的）");
            }
        }
    }

    // ---------- 版本迁移 ----------

    /**
     * 跑迁移链并写回文件。
     * <p>
     * 迁移前先把原文件备份成 {@code <名字>.v<旧版本>.bak}：迁移一旦写错，服主至少还能拿回原文件。
     * 迁移函数由 {@link MenuConfig#migrate()} 指名，签名是
     * {@code static void 名字(YamlConfiguration yaml)}——不带实例是因为它操作的是文件内容，
     * 而不是某个菜单实例的状态。
     */
    private static void migrate(Plugin plugin, MenuTemplate template, String path,
                                YamlConfiguration yaml, MenuConfig config, int fileVersion) {
        String method = config.migrate();
        if (method == null || method.trim().isEmpty()) {
            MenuLog.warn(path + " 的 version=" + fileVersion + " 低于代码的 version=" + config.version()
                    + "，但没有声明 migrate：文件按原样读取");
            return;
        }
        invokeMigration(template, method.trim(), yaml);
        yaml.set("version", Integer.valueOf(config.version()));

        if (path.startsWith(RES_PREFIX)) {
            // 内置只读配置：迁移结果不落地，只在本进程内生效
            return;
        }
        File file = fileOf(plugin, path);
        File backup = new File(file.getParentFile(), file.getName() + ".v" + fileVersion + ".bak");
        try {
            java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            yaml.save(file);
        } catch (Exception failure) {
            throw new MenuConfigException(path + " 迁移后写回失败: " + failure.getMessage(), failure);
        }
        MenuLog.warn(path + " 已从 version=" + fileVersion + " 迁移到 " + config.version()
                + "，原文件备份为 " + backup.getName());
    }

    /** 调用模板类上的迁移函数；找不到 / 签名不对都当场抛（那是代码 bug）。 */
    private static void invokeMigration(MenuTemplate template, String method, YamlConfiguration yaml) {
        Method found;
        try {
            found = template.getClass().getDeclaredMethod(method, YamlConfiguration.class);
        } catch (NoSuchMethodException missing) {
            throw new MenuConfigException(template.getClass().getName() + " 声明了 migrate=\"" + method
                    + "\"，但没有这个静态方法：期望签名是 static void " + method
                    + "(YamlConfiguration yaml)", missing);
        }
        try {
            found.setAccessible(true);
            found.invoke(null, yaml);
        } catch (ReflectiveOperationException failure) {
            throw new MenuConfigException(template.getClass().getName() + " 的迁移方法 " + method
                    + " 执行失败: " + failure.getCause(), failure);
        }
    }

    // ---------- 杂项 ----------

    private static File fileOf(Plugin plugin, String path) {
        String name = path.startsWith("/") || path.startsWith("\\") ? path.substring(1) : path;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (!lower.endsWith(".yml") && !lower.endsWith(".yaml")) {
            name = name + ".yml";
        }
        File folder = plugin == null ? new File(".") : plugin.getDataFolder();
        return new File(folder, name);
    }
}
