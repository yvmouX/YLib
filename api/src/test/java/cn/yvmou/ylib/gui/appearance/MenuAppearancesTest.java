package cn.yvmou.ylib.gui.appearance;

import cn.yvmou.ylib.gui.SlotLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外观配置的启动校验：布局能解析、材质认得出来。
 * <p>
 * 这两类错的表现都是「界面看起来不对」（那一格摆不上 / 图标静默变成纸），等玩家点开再排查就晚了，
 * 所以要在启动与重载时报出来，并且指明是哪个文件的哪个键。这里钉住的就是「报得准」：
 * 消息里必须出现文件名与键名，否则管理员手里只有文件，看到「material 认不出来」也不知道改哪儿。
 */
class MenuAppearancesTest {

    @Test
    @DisplayName("合格的配置什么都不做")
    void validPasses() {
        assertDoesNotThrow(() -> MenuAppearances.validate(appearance(
                new String[]{"#########", "##`close`##"}, items("close", "BARRIER"))));
    }

    @Test
    @DisplayName("布局为空 / 行数超容量 / 一行超 9 格：抛，并指明文件名")
    void malformedLayoutIsReported() {
        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> MenuAppearances.validate(appearance(new String[0], Map.of())));
        assertTrue(empty.getMessage().contains("test.main"), empty.getMessage());
        assertTrue(empty.getMessage().contains("layout"), empty.getMessage());

        IllegalArgumentException overWide = assertThrows(IllegalArgumentException.class,
                () -> MenuAppearances.validate(appearance(new String[]{"##########"}, Map.of())));
        assertTrue(overWide.getMessage().contains("layout"), overWide.getMessage());
        assertTrue(overWide.getMessage().contains("9"), overWide.getMessage());
    }

    @Test
    @DisplayName("材质认不出来：抛，并指明是哪个 items 键的 material")
    void unknownMaterialIsReported() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MenuAppearances.validate(appearance(
                        new String[]{"`close`"}, items("close", "NOT_A_MATERIAL"))));

        assertTrue(failure.getMessage().contains("items.close"), failure.getMessage());
        assertTrue(failure.getMessage().contains("material"), failure.getMessage());
    }

    @Test
    @DisplayName("物品表里某一条是 null：抛（至少要能读出 material）")
    void nullItemIsReported() {
        Map<String, MenuItemAppearance> items = new LinkedHashMap<>();
        items.put("close", null);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> MenuAppearances.validate(appearance(new String[]{"`close`"}, items)));
        assertTrue(failure.getMessage().contains("items.close"), failure.getMessage());
    }

    @Test
    @DisplayName("不要求「布局里每个名字都有物品」：* 这类占位字符只表示要背景板，本来就没有对应物品")
    void layoutNamesWithoutItemsAreAllowed() {
        assertDoesNotThrow(() -> MenuAppearances.validate(appearance(
                new String[]{"*********", "**`close`**"}, items("close", "BARRIER"))));
    }

    @Test
    @DisplayName("resolveMaterial 与 MenuItem 同一套规则（认不出来给 null，不退回纸）")
    void resolvesToNullWhenUnknown() {
        assertTrue(MenuAppearances.resolveMaterial("BARRIER") != null);
        assertTrue(MenuAppearances.resolveMaterial("NOT_A_MATERIAL") == null);
        assertTrue(MenuAppearances.resolveMaterial("") == null);
    }

    // ---------- 契约报告 ----------

    @Test
    @DisplayName("配置侧报告：每一格与每一处文案列出可用的占位符；从没构建过时说明这一点")
    void contractListsAvailablePlaceholders() {
        MenuAppearance appearance = MenuAppearances.create("&e测试")
                .path("test.contract")
                .layout("`buy`", "`info`")
                .item("buy", "DIAMOND", "&b买一个", "&7价格 {price}")
                .item("info", "PAPER", "&7说明")
                .text("entry-lore", "&8{id}", "&7数量 {amount}")
                .build();

        List<String> lines = MenuAppearances.contract(appearance);

        assertTrue(joined(lines).contains("items.buy -> [price]"), joined(lines));
        assertTrue(joined(lines).contains("items.info -> （无）"), joined(lines));
        assertTrue(joined(lines).contains("entry-lore -> [id, amount]"), joined(lines));
        assertTrue(joined(lines).contains("还没被打开过"), joined(lines));
    }

    @Test
    @DisplayName("运行侧报告：构建次数、「给了但没用上」、「配了但从没被摆过」")
    void contractReportsRuntimeSide() {
        MenuAppearance appearance = MenuAppearances.create("&e测试")
                .path("test.runtime")
                .layout("`buy`", "`unused`")
                .item("buy", "DIAMOND", "&b买一个", "&7价格 {price}")
                .item("unused", "PAPER", "&7没人摆")
                .text("entry-lore", "&8{id}")
                .build();
        Map<String, Set<String>> required = new LinkedHashMap<String, Set<String>>();
        required.put("gui.test.runtime.items.buy", setOf("price"));
        required.put("gui.test.runtime.entry-lore", setOf("id"));
        Map<String, Set<String>> provided = new LinkedHashMap<String, Set<String>>();
        provided.put("gui.test.runtime.items.buy", setOf("price", "amount"));
        provided.put("gui.test.runtime.entry-lore", setOf("id"));

        MenuAppearances.record(appearance, required, provided);
        MenuAppearances.record(appearance, required, provided);
        List<String> lines = MenuAppearances.contract(appearance);
        String joined = joined(lines);

        assertTrue(joined.contains("打开过 2 次"), joined);
        assertTrue(joined.contains("给了但当前配置没用上：[amount]"), joined);
        assertTrue(joined.contains("配了但从没被摆过：[items.unused]"), joined);
        assertEquals(setOf("amount"), MenuAppearances.record("test.runtime").unused());
    }

    @Test
    @DisplayName("对账开关默认：开着检查、但不抛（默认只在日志里说）")
    void defaultsAreLenient() {
        assertTrue(MenuAppearances.checkContract());
        assertFalse(MenuAppearances.strictContract());
    }

    @Test
    @DisplayName("代码造的外观能直接用：标题 / 布局 / 物品 / 文案都取得到，文件名给「代码里的外观」")
    void codeBuiltAppearanceIsUsable() {
        MenuAppearance appearance = MenuAppearances.create("&e商店")
                .layout("#########", "#`buy`####", "#########")
                .item("buy", "DIAMOND", "&b买一个", "&7价格 100")
                .text("note", "&7提示")
                .build();

        assertDoesNotThrow(() -> MenuAppearances.validate(appearance));
        assertEquals("&e商店", appearance.title());
        assertEquals(27, SlotLayout.sizeOf(appearance.layout()));
        assertEquals("DIAMOND", appearance.items().get("buy").material());
        assertEquals(listOf("&7提示"), appearance.texts().get("note"));
        assertEquals("代码里的外观", appearance.fileName());
    }

    private static String joined(List<String> lines) {
        return String.join("\n", lines);
    }

    private static Set<String> setOf(String... names) {
        return new LinkedHashSet<String>(Arrays.asList(names));
    }

    private static List<String> listOf(String... lines) {
        return Arrays.asList(lines);
    }

    // ---------- 替身 ----------

    private static MenuAppearance appearance(String[] layout, Map<String, MenuItemAppearance> items) {
        return new FakeAppearance(layout, items);
    }

    private static Map<String, MenuItemAppearance> items(String name, String material) {
        Map<String, MenuItemAppearance> items = new LinkedHashMap<>();
        items.put(name, new FakeItem(material));
        return items;
    }

    private static final class FakeAppearance implements MenuAppearance {

        private final String[] layout;
        private final Map<String, MenuItemAppearance> items;

        FakeAppearance(String[] layout, Map<String, MenuItemAppearance> items) {
            this.layout = layout;
            this.items = items;
        }

        @Override
        public String path() {
            return "test.main";
        }

        @Override
        public String title() {
            return "测试";
        }

        @Override
        public String[] layout() {
            return layout;
        }

        @Override
        public Map<String, ? extends MenuItemAppearance> items() {
            return items;
        }
    }

    private static final class FakeItem implements MenuItemAppearance {

        private final String material;

        FakeItem(String material) {
            this.material = material;
        }

        @Override
        public String material() {
            return material;
        }

        @Override
        public String name() {
            return " ";
        }

        @Override
        public List<String> lore() {
            return List.of();
        }
    }
}
