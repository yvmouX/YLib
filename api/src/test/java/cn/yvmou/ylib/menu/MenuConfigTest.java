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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        b.symbol('#').item("frame").of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "));
        b.symbol("buy").item("buy_btn")
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
}
