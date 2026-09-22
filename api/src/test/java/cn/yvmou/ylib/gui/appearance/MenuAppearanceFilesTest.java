package cn.yvmou.ylib.gui.appearance;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 外观文件层（{@code MenuAppearances.file}）：代码定义默认值，YLib 生成与读取文件。
 * <p>
 * 钉住的契约：缺文件按默认值生成（带注释）；文件里写了的以文件为准、缺的键回落默认值；
 * 写坏的键只回退它自己并记警告（警告带文件名）；{@code reloadFiles} 之后读到磁盘上的新值；
 * 默认值本身是坏的（代码 bug）当场抛。
 * <p>
 * 警告统一收到 {@link MenuAppearances#logger} 里断言（不接的话会落到 Bukkit 控制台，单测里那是 null）。
 */
class MenuAppearanceFilesTest {

    @TempDir
    Path dataFolder;

    private final List<String> warnings = new ArrayList<>();

    @BeforeEach
    void captureWarnings() {
        warnings.clear();
        MenuAppearances.logger(warnings::add);
    }

    // ---------- 生成与读回 ----------

    @Test
    @DisplayName("缺文件：按默认值生成一份带注释的配置（title/layout/items/texts 齐全）")
    void generatesDefaults() throws Exception {
        MenuAppearance appearance = register(plugin(), "gui/shop", shopDefaults());

        Path file = dataFolder.resolve("gui/shop.yml");
        assertTrue(Files.exists(file), "文件没生成出来");
        String yaml = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(yaml.contains("title: '&e商店'"), "标题没写进去:" + System.lineSeparator() + yaml);
        assertTrue(yaml.contains("layout:"), "布局没写进去:" + System.lineSeparator() + yaml);
        assertTrue(yaml.contains("buy:") && yaml.contains("material: DIAMOND"), "物品表没写成嵌套结构:" + System.lineSeparator() + yaml);
        assertTrue(yaml.contains("entry-lore:"), "数据文案没写进去:" + System.lineSeparator() + yaml);
        assertTrue(warnings.isEmpty(), "干净文件不该有警告: " + warnings);
        assertEquals(shopDefaults().title(), appearance.title());
    }

    @Test
    @DisplayName("重载读回：生成的文件重新读一遍，内容与原默认值一致")
    void reloadReadsBackWhatWasGenerated() {
        MenuAppearance registered = register(plugin(), "gui/shop-roundtrip", shopDefaults());

        MenuAppearances.reloadFiles();

        assertEquals(shopDefaults().title(), registered.title());
        assertArrayEquals(shopDefaults().layout(), registered.layout());
        assertEquals(shopDefaults().items().keySet(), registered.items().keySet());
        for (String name : shopDefaults().items().keySet()) {
            assertEquals(shopDefaults().items().get(name).material(), registered.items().get(name).material(), name);
            assertEquals(shopDefaults().items().get(name).name(), registered.items().get(name).name(), name);
            assertEquals(shopDefaults().items().get(name).lore(), registered.items().get(name).lore(), name);
        }
        assertEquals(shopDefaults().texts(), registered.texts());
        assertTrue(warnings.isEmpty(), "回读不该有警告: " + warnings);
    }

    // ---------- 文件优先、缺键回落 ----------

    @Test
    @DisplayName("文件优先：写了的覆盖默认值，没写的回落默认值；文件独有的条目也收进来")
    void fileWinsMissingKeysFallBack() throws Exception {
        write("gui/shop-override.yml", ""
                + "title: 自定义商店" + System.lineSeparator()
                + "items:" + System.lineSeparator()
                + "  buy:" + System.lineSeparator()
                + "    material: EMERALD" + System.lineSeparator()
                + "  extra:" + System.lineSeparator()
                + "    material: BARRIER" + System.lineSeparator()
                + "    name: 额外的格子" + System.lineSeparator()
                + "entry-lore:" + System.lineSeparator()
                + "- '&8只有一行'" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-override", shopDefaults());

        assertEquals("自定义商店", appearance.title(), "文件里的标题要赢");
        assertEquals("EMERALD", appearance.items().get("buy").material(), "文件里的材质要赢");
        assertEquals("&b买一颗钻石", appearance.items().get("buy").name(), "文件没写的 name 回落默认值");
        assertEquals(List.of("&7价格 {price}"), appearance.items().get("buy").lore(), "文件没写的 lore 回落默认值");
        assertNotNull(appearance.items().get("extra"), "文件里独有的条目要收进来");
        assertEquals("BARRIER", appearance.items().get("extra").material());
        assertArrayEquals(shopDefaults().layout(), appearance.layout(), "文件没写 layout 就用默认布局");
        assertEquals(List.of("&8只有一行"), appearance.texts().get("entry-lore"));
        assertEquals(shopDefaults().texts().get("empty-note"), appearance.texts().get("empty-note"),
                "文件没写的文案回落默认值");
    }

    // ---------- 写坏回退 ----------

    @Test
    @DisplayName("布局写坏（一行超 9 格）：回退默认布局，警告指明文件")
    void brokenLayoutFallsBack() throws Exception {
        write("gui/shop-bad-layout.yml", ""
                + "layout:" + System.lineSeparator()
                + "- '##########'" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-bad-layout", shopDefaults());

        assertArrayEquals(shopDefaults().layout(), appearance.layout(), "坏布局要回退默认");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("gui/shop-bad-layout.yml") && w.contains("layout")),
                "警告要指明文件与键: " + warnings);
        // 布局坏了，物品与文案仍按文件/默认合并：不是整个文件作废
        assertEquals("&b买一颗钻石", appearance.items().get("buy").name());
    }

    @Test
    @DisplayName("材质认不出来：那一格回退默认材质并警告，其余自定义不丢")
    void unknownMaterialFallsBackPerItem() throws Exception {
        write("gui/shop-bad-material.yml", ""
                + "items:" + System.lineSeparator()
                + "  buy:" + System.lineSeparator()
                + "    material: NOT_A_MATERIAL" + System.lineSeparator()
                + "    name: 自定义名字" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-bad-material", shopDefaults());

        assertEquals("DIAMOND", appearance.items().get("buy").material(), "坏材质回退默认");
        assertEquals("自定义名字", appearance.items().get("buy").name(), "同一格里写对了的要保留");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("items.buy.material") && w.contains("NOT_A_MATERIAL")),
                "警告要指明是哪个物品的哪个键: " + warnings);
    }

    @Test
    @DisplayName("文件独有的条目材质也是坏的：整条不生效，但不让加载失败")
    void brokenFileOnlyItemIsDropped() throws Exception {
        write("gui/shop-bad-extra.yml", ""
                + "items:" + System.lineSeparator()
                + "  extra:" + System.lineSeparator()
                + "    name: 没有材质" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-bad-extra", shopDefaults());

        assertTrue(!appearance.items().containsKey("extra"), "文件独有且材质缺失的条目不该进表");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("items.extra")), "警告要提到这条: " + warnings);
    }

    @Test
    @DisplayName("不认识的顶层键：记警告（可能是旧版本留下的），加载照常")
    void unknownTopLevelKeyIsReported() throws Exception {
        write("gui/shop-stale-key.yml", ""
                + "sort:" + System.lineSeparator()
                + "  modes:" + System.lineSeparator()
                + "    id: asc" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-stale-key", shopDefaults());

        assertEquals(shopDefaults().title(), appearance.title(), "多出来的键不影响其余部分");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("sort")), "警告要指出是哪个键: " + warnings);
    }

    @Test
    @DisplayName("文件整个读不出来：回退代码默认值并警告，不抛")
    void unreadableFileFallsBackToDefaults() throws Exception {
        write("gui/shop-unreadable.yml", "title: [没闭合" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-unreadable", shopDefaults());

        assertEquals(shopDefaults().title(), appearance.title());
        assertArrayEquals(shopDefaults().layout(), appearance.layout());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("gui/shop-unreadable.yml") && w.contains("读不出来")),
                "警告要说明文件读不出来: " + warnings);
    }

    // ---------- 边界 ----------

    @Test
    @DisplayName("数据文案单行可写成标量、多行写成列表，两种形态都认")
    void textsAcceptScalarAndList() throws Exception {
        write("gui/shop-text-forms.yml", ""
                + "entry-lore: '&8一行就够'" + System.lineSeparator()
                + "empty-note:" + System.lineSeparator()
                + "- '&7第一行'" + System.lineSeparator()
                + "- '&7第二行'" + System.lineSeparator());

        MenuAppearance appearance = register(plugin(), "gui/shop-text-forms", shopDefaults());

        assertEquals(List.of("&8一行就够"), appearance.texts().get("entry-lore"));
        assertEquals(List.of("&7第一行", "&7第二行"), appearance.texts().get("empty-note"));
    }

    @Test
    @DisplayName("默认值本身是坏的（空布局）：当场抛——这是代码 bug，不是配置问题")
    void brokenDefaultsThrow() {
        MenuAppearance broken = MenuAppearances.create("坏菜单").path("test.broken").build();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> register(plugin(), "gui/broken", broken));
        assertTrue(e.getMessage().contains("layout"), e.getMessage());
    }

    @Test
    @DisplayName("默认值没写 path：当场抛（语言键前缀必须有）")
    void missingPathThrows() {
        MenuAppearance noPath = MenuAppearances.create("没名字").layout("#########").build();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> register(plugin(), "gui/no-path", noPath));
        assertTrue(e.getMessage().contains("path"), e.getMessage());
    }

    @Test
    @DisplayName("不写扩展名自动补 .yml；重复登记同一文件返回同一个实例")
    void normalizeAndIdempotent() throws Exception {
        MenuAppearance first = register(plugin(), "gui/shop-idem", shopDefaults());
        MenuAppearance second = register(plugin(), "gui/shop-idem.yml", shopDefaults());

        assertTrue(first == second, "同一文件只登记一次");
        assertTrue(Files.exists(dataFolder.resolve("gui/shop-idem.yml")), "自动补了 .yml");
    }

    // ---------- 辅助 ----------

    /** 商店外观：两份物品、两处数据文案，覆盖默认值的全部形态。 */
    private static MenuAppearance shopDefaults() {
        return MenuAppearances.create("&e商店")
                .path("test.shop")
                .layout("#########", "#`buy`#####", "#########")
                .item("buy", "DIAMOND", "&b买一颗钻石", "&7价格 {price}")
                .item(MenuAppearance.FILLER, "GRAY_STAINED_GLASS_PANE", " ")
                .text("entry-lore", "&8{id}", "&7点我")
                .text("empty-note", "&7还没有东西")
                .build();
    }

    private Plugin plugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        return plugin;
    }

    private MenuAppearance register(Plugin plugin, String fileName, MenuAppearance defaults) {
        return MenuAppearances.file(plugin, fileName, defaults);
    }

    private void write(String relative, String content) throws Exception {
        Path target = dataFolder.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
