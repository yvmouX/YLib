package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuLog;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置文件层（重构文档 §9）：**代码给默认值、文件负责覆盖**。
 * <p>
 * 用反射代理造一个最小 {@code Plugin}（只要 {@code getDataFolder} 与 {@code getResource}），
 * 因此这一层的规则——生成、合并、版本迁移、脏数据回退——都能在没有服务端的环境里钉死。
 */
class MenuConfigTest {

    private static final List<String> WARNINGS = new ArrayList<String>();

    @BeforeAll
    static void setUp() {
        TestServer.install();
        MenuLog.sink(new java.util.function.Consumer<String>() {
            @Override
            public void accept(String message) {
                WARNINGS.add(message);
            }
        });
    }

    // ---------- 用例辅助 ----------

    /** 只要 getDataFolder / getResource 的假插件。 */
    private static org.bukkit.plugin.Plugin plugin(final File folder) {
        return (org.bukkit.plugin.Plugin) Proxy.newProxyInstance(
                MenuConfigTest.class.getClassLoader(),
                new Class<?>[]{org.bukkit.plugin.Plugin.class},
                (proxy, method, args) -> {
                    if ("getDataFolder".equals(method.getName())) {
                        return folder;
                    }
                    if ("getResource".equals(method.getName())) {
                        return null;
                    }
                    if ("getName".equals(method.getName())) {
                        return "YLibMenuTest";
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestPlugin";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == void.class) {
            return null;
        }
        if (type == char.class) {
            return Character.valueOf('\0');
        }
        if (type == byte.class) {
            return Byte.valueOf((byte) 0);
        }
        if (type == short.class) {
            return Short.valueOf((short) 0);
        }
        if (type == int.class) {
            return Integer.valueOf(0);
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        if (type == float.class) {
            return Float.valueOf(0F);
        }
        return Double.valueOf(0D);
    }

    private static File write(File folder, String name, String content) throws Exception {
        File file = new File(folder, name);
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static YamlConfiguration load(File file) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        return yaml;
    }

    // ---------- 模板 ----------

    /**
     * 商店的布局。
     * <p>
     * 多字符符号按解析器口径占 <b>1</b> 格，因此第二行 {@code "####`buy`####"} 是 4 + 1 + 4 = 9 格。
     */
    private static final String[] SHOP_LAYOUT =
            {"#########", "####`buy`####", "#########", "#########"};

    private static void declareShop(MenuBuilder b) {
        b.symbol('#', "frame").of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "));
        b.symbol("buy", "buy_btn")
                .of(Items.of(Material.DIAMOND).name("&b买一颗钻石").lore("&7价格 {price}"))
                .ph("price", new java.util.function.Function<MenuContext, String>() {
                    @Override
                    public String apply(MenuContext ctx) {
                        return "100";
                    }
                });
    }

    /** 没注解的模板：完全不碰文件。 */
    private static MenuTemplate plainShop() {
        return new MenuTemplate() {
            @Override
            public String[] layout() {
                return SHOP_LAYOUT;
            }

            @Override
            public void declare(MenuBuilder b) {
                declareShop(b);
            }
        };
    }

    /** 带 {@code @MenuConfig} 的商店模板：真正会读写 gui/shop.yml。 */
    @MenuConfig(path = "gui/shop.yml", version = 1)
    public static final class AnnotatedShop implements MenuTemplate {

        @Override
        public String[] layout() {
            return SHOP_LAYOUT;
        }

        @Override
        public void declare(MenuBuilder b) {
            declareShop(b);
        }
    }

    /** 声明了迁移函数、且版本号是 1（用来验证「旧文件被迁移」）。 */
    @MenuConfig(path = "gui/shop.yml", version = 1, migrate = "migrateV0")
    public static final class MigratingShop implements MenuTemplate {
        @Override
        public String[] layout() {
            return SHOP_LAYOUT;
        }

        @Override
        public void declare(MenuBuilder b) {
            declareShop(b);
        }

        /** 迁移函数：签名必须是 {@code static void 名字(YamlConfiguration)}。 */
        @SuppressWarnings("unused")
        static void migrateV0(YamlConfiguration yaml) {
            if (yaml.getString("items.buy_btn.name") != null) {
                yaml.set("items.buy_btn.name", "&a迁移后的名字");
            }
        }
    }

    /** 声明了 migrate 却没写这个方法：应当是代码 bug，启动就炸。 */
    @MenuConfig(path = "gui/shop.yml", version = 1, migrate = "noSuchMethod")
    public static final class BrokenMigrationShop implements MenuTemplate {

        @Override
        public String[] layout() {
            return SHOP_LAYOUT;
        }

        @Override
        public void declare(MenuBuilder b) {
            declareShop(b);
        }
    }

    /**
     * {@code generateDefault = false}：文件的生成与回写都交给运维 / 版本控制。
     * 库仍然**读**它（缺的键回落代码默认值），但不该往里写任何东西。
     */
    @MenuConfig(path = "gui/shop.yml", version = 1, generateDefault = false)
    public static final class NoGenerateShop implements MenuTemplate {

        @Override
        public String[] layout() {
            return SHOP_LAYOUT;
        }

        @Override
        public void declare(MenuBuilder b) {
            declareShop(b);
        }
    }

    // ---------- 生成 ----------

    @Test
    @DisplayName("缺文件：按代码默认值生成一份带 version / items 的配置")
    void generatesDefaults(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        File generated = new File(folder, "gui/shop.yml");
        assertTrue(generated.isFile(), "缺文件时应当生成默认配置");

        YamlConfiguration yaml = load(generated);
        assertEquals(1, yaml.getInt("version"));
        assertEquals("GRAY_STAINED_GLASS_PANE", yaml.getString("items.frame.material"));
        assertEquals("DIAMOND", yaml.getString("items.buy_btn.material"));
        assertEquals("&b买一颗钻石", yaml.getString("items.buy_btn.name"));
        assertEquals(Arrays.asList("&7价格 {price}"), yaml.getStringList("items.buy_btn.lore"));
        // 生成完就能用：没被文件改动的符号仍是代码默认值
        assertEquals(Material.DIAMOND, definition.stateByConfigKey("buy_btn").items().material());
    }

    @Test
    @DisplayName("生成的配置不带注释（说明在文档里，文件保持干净）")
    void generatedFileHasNoComments(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        Menus.register(plugin(folder), new AnnotatedShop());

        File generated = new File(folder, "gui/shop.yml");
        assertTrue(generated.isFile(), "缺文件时应当生成默认配置");

        for (String line : Files.readAllLines(generated.toPath(), StandardCharsets.UTF_8)) {
            assertFalse(line.trim().startsWith("#"),
                    "生成的文件里不该有注释，但出现了：" + line);
        }

        // 内容该有的还得有：布局是**真键**（不是注释里的参考图），这样它才真的可改
        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals(Arrays.asList(SHOP_LAYOUT), yaml.getStringList("layout"),
                "layout 应当是可读回的真键");
        assertEquals("DIAMOND", yaml.getString("items.buy_btn.material"));
    }

    // ---------- 布局覆盖 ----------

    @Test
    @DisplayName("文件里的 layout 覆盖代码：容器大小与每个符号的格子都跟着变")
    void fileOverridesLayout(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\n"
                + "layout:\n"
                + "- '#########'\n"
                + "- '####`buy`####'\n");
        Menus.shutdown();

        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        assertEquals(18, definition.size(), "布局改成 2 行，容器就是 18 格");
        assertEquals(13, definition.slotsOf("buy").get(0).index(), "buy 落在第 2 行第 5 格");
        assertEquals(List.of(), definition.warnings(), "覆盖后的布局里符号都有格子");
    }

    @Test
    @DisplayName("文件里的 layout 写坏了：整份回退代码布局 + 警告，注册照常成功")
    void brokenLayoutFallsBackAsAWhole(@TempDir Path temp) throws Exception {
        // 第 2 行只有 8 格
        write(temp.toFile(), "gui/shop.yml", "version: 1\n"
                + "layout:\n"
                + "- '#########'\n"
                + "- '#`buy`######'\n");
        Menus.shutdown();
        MenuDefinition tooNarrow = Menus.register(plugin(temp.toFile()), new AnnotatedShop());
        assertEquals(36, tooNarrow.size(), "格数不对 → 整份回退代码里的 4 行布局");

        // 引用了没在 declare() 里声明过的符号
        write(temp.toFile(), "gui/shop.yml", "version: 1\n"
                + "layout:\n"
                + "- '#########'\n"
                + "- '####`nope`###'\n");
        Menus.shutdown();
        MenuDefinition unknownSymbol = Menus.register(plugin(temp.toFile()), new AnnotatedShop());
        assertEquals(36, unknownSymbol.size());
        assertEquals(13, unknownSymbol.slotsOf("buy").get(0).index(), "回退后用的是代码布局");
    }

    @Test
    @DisplayName("老文件里没有 layout：补上代码里那一份，已有的值不动")
    void layoutIsFilledIntoOlderFiles(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems:\n  buy_btn:\n    material: EMERALD\n");
        Menus.shutdown();

        Menus.register(plugin(folder), new AnnotatedShop());

        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals("EMERALD", yaml.getString("items.buy_btn.material"), "已有的值必须保留");
        assertEquals(Arrays.asList(SHOP_LAYOUT), yaml.getStringList("layout"),
                "缺的 layout 应当按代码默认值补进文件");
    }

    @Test
    @DisplayName("已存在的文件绝不被覆盖")
    void neverOverwritesExistingFile(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems:\n  buy_btn:\n    material: EMERALD\n");
        Menus.shutdown();

        Menus.register(plugin(folder), new AnnotatedShop());
        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals("EMERALD", yaml.getString("items.buy_btn.material"), "已存在的文件必须原样保留");
    }

    // ---------- 合并 ----------

    @Test
    @DisplayName("文件优先：写了的以文件为准，缺的字段回落代码默认值")
    void fileOverridesCode(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: EMERALD\n"
                + "    name: '&a自定义名字'\n");
        Menus.shutdown();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        Items merged = definition.stateByConfigKey("buy_btn").items();
        assertEquals(Material.EMERALD, merged.material(), "material 被文件覆盖");
        assertEquals("&a自定义名字", merged.name(), "name 被文件覆盖");
        assertEquals(Arrays.asList("&7价格 {price}"), merged.lore(), "lore 回落代码默认值");
    }

    @Test
    @DisplayName("材质认不出来：那一项回退代码默认材质并记警告，其余自定义不丢")
    void badMaterialFallsBack(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: NOT_A_MATERIAL\n"
                + "    name: '&a还是我的名字'\n");
        Menus.shutdown();
        WARNINGS.clear();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        Items merged = definition.stateByConfigKey("buy_btn").items();
        assertEquals(Material.DIAMOND, merged.material(), "坏材质回退代码默认值");
        assertEquals("&a还是我的名字", merged.name(), "同一项里的其它字段仍然生效");
        boolean warned = false;
        for (String warning : WARNINGS) {
            if (warning.contains("material")) {
                warned = true;
            }
        }
        assertTrue(warned, WARNINGS.toString());
    }

    @Test
    @DisplayName("文件里布局中不存在的键：忽略并记警告")
    void unknownItemKeyWarns(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems:\n  nope:\n    material: STONE\n");
        Menus.shutdown();
        WARNINGS.clear();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        assertNotNull(definition.stateByConfigKey("buy_btn"));
        boolean warned = false;
        for (String warning : WARNINGS) {
            if (warning.contains("nope")) {
                warned = true;
            }
        }
        assertTrue(warned, WARNINGS.toString());
    }

    @Test
    @DisplayName("布局里有符号但 yml 没有：用代码默认值，不报错")
    void missingItemUsesDefaults(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems: {}\n");
        Menus.shutdown();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                definition.stateByConfigKey("frame").items().material());
        assertEquals(Material.DIAMOND, definition.stateByConfigKey("buy_btn").items().material());
    }

    @Test
    @DisplayName("yml 里配了 actions：接管该符号的点击行为，并提示代码里的 onClick 被忽略")
    void actionsTakeOverBehaviour(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: DIAMOND\n"
                + "    actions:\n"
                + "      - type: message\n"
                + "        text: '&7共 {price} 个'\n");
        Menus.shutdown();
        WARNINGS.clear();
        MenuDefinition definition = Menus.register(plugin(folder), new AnnotatedShop());

        // actions 落在 fallbackClick 上：没注册过 onClick 的符号因此「有了点击」
        assertTrue(definition.stateByConfigKey("buy_btn").hasClick(),
                "配了 actions 之后这一格应当有点击回调");
    }

    // ---------- 版本 ----------

    @Test
    @DisplayName("yml 版本比代码新：拒绝加载该文件")
    void newerVersionIsRejected(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 99\nitems: {}\n");
        Menus.shutdown();
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> Menus.register(plugin(folder), new AnnotatedShop()));
        assertTrue(failure.getMessage().contains("version=99"), failure.getMessage());
        assertTrue(failure.getMessage().contains("请升级插件"), failure.getMessage());
    }

    @Test
    @DisplayName("yml 版本比代码旧：跑迁移函数、备份原文件、写回新版本号")
    void olderVersionIsMigrated(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 0\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    name: '&a老名字'\n");
        Menus.shutdown();

        MenuDefinition definition = Menus.register(plugin(folder), new MigratingShop());
        assertEquals("&a迁移后的名字", definition.stateByConfigKey("buy_btn").items().name());

        assertTrue(new File(folder, "gui/shop.yml.v0.bak").isFile(), "迁移前必须备份原文件");
        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals(1, yaml.getInt("version"));
        assertEquals("&a迁移后的名字", yaml.getString("items.buy_btn.name"));
    }

    @Test
    @DisplayName("声明了 migrate 但方法不存在：当场抛（那是代码 bug）")
    void missingMigrationMethodFails(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 0\nitems: {}\n");
        Menus.shutdown();
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> Menus.register(plugin(folder), new BrokenMigrationShop()));
        assertTrue(failure.getMessage().contains("noSuchMethod"), failure.getMessage());
        assertTrue(failure.getMessage().contains("静态方法"), failure.getMessage());
    }

    // ---------- 升级友好：补齐新增键 ----------

    @Test
    @DisplayName("老配置文件缺了新版本的符号键：自动补进文件（这是升级场景的关键一条）")
    void fillsMissingItemKeys(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        // 一份「上个版本」的配置：只有 frame，没有 buy_btn
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  frame:\n"
                + "    material: GRAY_STAINED_GLASS_PANE\n"
                + "    name: ' '\n");
        Menus.shutdown();
        WARNINGS.clear();
        Menus.register(plugin(folder), new AnnotatedShop());

        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        // 新符号以代码默认值补了进去
        assertEquals("DIAMOND", yaml.getString("items.buy_btn.material"));
        assertEquals("&b买一颗钻石", yaml.getString("items.buy_btn.name"));
        assertEquals(Arrays.asList("&7价格 {price}"), yaml.getStringList("items.buy_btn.lore"));
        // 用户原有的项一个字都没动
        assertEquals("GRAY_STAINED_GLASS_PANE", yaml.getString("items.frame.material"));
        assertEquals(" ", yaml.getString("items.frame.name"));
        // 补了东西要出声，否则「配置自己变了」很吓人
        boolean warned = false;
        for (String warning : WARNINGS) {
            if (warning.contains("补齐")) {
                warned = true;
            }
        }
        assertTrue(warned, WARNINGS.toString());
    }

    @Test
    @DisplayName("补充不覆盖用户改过的值，也不动用户写的注释")
    void fillDoesNotTouchUserValues(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\n"
                + "# 我自己写的注释，别动它\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: EMERALD\n"
                + "    name: '&a我改过的名字'\n"
                + "    lore:\n"
                + "    - '&7我的说明'\n");
        Menus.shutdown();
        Menus.register(plugin(folder), new AnnotatedShop());

        String text = new String(Files.readAllBytes(new File(folder, "gui/shop.yml").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(text.contains("# 我自己写的注释，别动它"), "用户的注释不该被覆盖：\n" + text);
        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals("EMERALD", yaml.getString("items.buy_btn.material"), "用户改过的值不该被改回默认");
        assertEquals("&a我改过的名字", yaml.getString("items.buy_btn.name"));
        assertEquals(Arrays.asList("&7我的说明"), yaml.getStringList("items.buy_btn.lore"));
    }

    @Test
    @DisplayName("项在但个别字段缺：只补缺的那个字段")
    void fillsMissingFieldOnly(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        // 有 material 和 name，故意删掉 lore
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: EMERALD\n"
                + "    name: '&a留着'\n"
                + "  frame:\n"
                + "    material: GRAY_STAINED_GLASS_PANE\n"
                + "    name: ' '\n"
                + "    lore: []\n");
        Menus.shutdown();
        Menus.register(plugin(folder), new AnnotatedShop());

        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertEquals("EMERALD", yaml.getString("items.buy_btn.material"), "已有的字段不该动");
        assertEquals("&a留着", yaml.getString("items.buy_btn.name"), "已有的字段不该动");
        assertEquals(Arrays.asList("&7价格 {price}"), yaml.getStringList("items.buy_btn.lore"),
                "缺掉的字段应当补上");
    }

    @Test
    @DisplayName("文件已完整：不写盘（mtime 不变）")
    void doesNotRewriteCompleteFile(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        Menus.register(plugin(folder), new AnnotatedShop());
        File file = new File(folder, "gui/shop.yml");

        long before = file.lastModified();
        // 停一下，让「有没有写盘」在时间戳上看得出来
        Thread.sleep(20);
        Menus.shutdown();
        Menus.register(plugin(folder), new AnnotatedShop());
        assertEquals(before, file.lastModified(),
                "文件已完整时不该重写——否则每次启动 mtime 都变，服主会以为配置被动过");
    }

    @Test
    @DisplayName("generateDefault = false：缺键只记警告，不写盘")
    void doesNotFillWhenGenerateDefaultOff(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems: {}\n");
        Menus.shutdown();
        WARNINGS.clear();
        Menus.register(plugin(folder), new NoGenerateShop());

        YamlConfiguration yaml = load(new File(folder, "gui/shop.yml"));
        assertNull(yaml.getString("items.buy_btn.material"),
                "关掉 generateDefault 表示这个文件由运维管，库不该往里写");
        boolean warned = false;
        for (String warning : WARNINGS) {
            if (warning.contains("generateDefault = false")) {
                warned = true;
            }
        }
        assertTrue(warned, "至少要说一声「缺了但我不写」：" + WARNINGS);
    }

    @Test
    @DisplayName("没有 @MenuConfig 的模板：完全不走文件，也不生成文件")
    void withoutAnnotationNoFileIsRead(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        MenuDefinition definition = Menus.register(plugin(folder), plainShop());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                definition.stateByConfigKey("frame").items().material());
        assertTrue(!new File(folder, "gui/shop.yml").exists(), "没注解就不该生成文件");
    }

    @Test
    @DisplayName("yml 语法错误：启动失败并打印原因（不静默降级）")
    void brokenYamlFails(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        write(folder, "gui/shop.yml", "version: 1\nitems:\n  - this is: not a section\n   bad indent\n");
        Menus.shutdown();
        assertThrows(MenuConfigException.class,
                () -> Menus.register(plugin(folder), new AnnotatedShop()));
    }

    @Test
    @DisplayName("同一个模板类注册两次：返回第一次那份定义")
    void registeringTwiceReturnsSameDefinition(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);
        MenuDefinition first = Menus.register(plugin, new AnnotatedShop());
        MenuDefinition second = Menus.register(plugin, new AnnotatedShop());
        assertTrue(first == second, "同一模板类只该有一份定义");
    }

    // ---------- 热重载 ----------

    @Test
    @DisplayName("reload 重读文件并换上一份新定义")
    void reloadRebuildsDefinition(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);

        MenuDefinition before = Menus.register(plugin, new AnnotatedShop());
        assertEquals(Material.DIAMOND, before.stateByConfigKey("buy_btn").items().material());

        // 改配置，然后重载
        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: EMERALD\n"
                + "    name: '&a热重载后的名字'\n");
        MenuDefinition after = Menus.reload(plugin, new AnnotatedShop());

        assertNotNull(after, "已注册的模板重载后应当拿到新定义");
        assertTrue(after != before, "重载应当换上一份**新**定义，而不是改旧的");
        assertEquals(Material.EMERALD, after.stateByConfigKey("buy_btn").items().material());
        assertEquals("&a热重载后的名字", after.stateByConfigKey("buy_btn").items().name());
        // 词典也指向新定义：之后 open 用到的就是它
        assertTrue(Menus.definitionOf(new AnnotatedShop()) == after, "定义表应当已切到新定义");
    }

    @Test
    @DisplayName("reload 没写的那一项仍然回落代码默认值")
    void reloadKeepsCodeDefaults(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);
        Menus.register(plugin, new AnnotatedShop());

        write(folder, "gui/shop.yml", "version: 1\n"
                + "items:\n"
                + "  buy_btn:\n"
                + "    material: EMERALD\n");
        MenuDefinition after = Menus.reload(plugin, new AnnotatedShop());

        // lore 没写 → 仍是代码里的那份（与启动期同一条合并路径）
        assertEquals(Arrays.asList("&7价格 {price}"), after.stateByConfigKey("buy_btn").items().lore());
        // 文件里没提的符号也还在
        assertEquals(Material.GRAY_STAINED_GLASS_PANE,
                after.stateByConfigKey("frame").items().material());
    }

    @Test
    @DisplayName("reload 没注册过的模板：返回 null，不凭空注册")
    void reloadUnknownTemplateReturnsNull(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        assertNull(Menus.reload(plugin(folder), new AnnotatedShop()));
        assertNull(Menus.definitionOf(new AnnotatedShop()), "重载不该顺手注册一个菜单");
    }

    @Test
    @DisplayName("reloadAll 重载全部已注册的菜单，并返回数量")
    void reloadAllReloadsEverything(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);
        Menus.register(plugin, new AnnotatedShop());
        Menus.register(plugin, plainShop());

        write(folder, "gui/shop.yml", "version: 1\nitems:\n  buy_btn:\n    material: EMERALD\n");
        assertEquals(2, Menus.reloadAll(plugin), "两个已注册的菜单都该被重载");
        assertEquals(Material.EMERALD,
                Menus.definitionOf(new AnnotatedShop()).stateByConfigKey("buy_btn").items().material());
    }

    @Test
    @DisplayName("reload 遇到版本比代码新的配置：照启动期那样抛")
    void reloadSurfacesBlockingConfigErrors(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);
        Menus.register(plugin, new AnnotatedShop());

        write(folder, "gui/shop.yml", "version: 99\nitems: {}\n");
        assertThrows(MenuConfigException.class, () -> Menus.reload(plugin, new AnnotatedShop()),
                "阻断级配置错误在重载时也应当抛，由调用方决定怎么报");
    }

    @Test
    @DisplayName("重载后 session 还在：菜单身份是模板类名，不随定义换掉")
    void reloadKeepsSessionIdentity(@TempDir Path temp) throws Exception {
        File folder = temp.toFile();
        Menus.shutdown();
        org.bukkit.plugin.Plugin plugin = plugin(folder);
        MenuDefinition before = Menus.register(plugin, new AnnotatedShop());
        MenuDefinition after = Menus.reload(plugin, new AnnotatedShop());
        assertEquals(before.key(), after.key(),
                "定义换了但身份不变，否则玩家会在重载后丢掉页码");
    }
}
