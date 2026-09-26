package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.MenuLog;
import cn.yvmou.ylib.menu.internal.SymbolState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 声明与校验：重构文档 §5「启动期校验清单」里那些**只靠模板就能判**的条目。
 * <p>
 * 这一层的价值全在「什么时候抛」：菜单写错必须在注册期炸，而不是等玩家点开时炸。
 * 因此每条校验都断言「抛了，而且报错信息指到了具体的位置 / 符号」。
 */
class MenuBuilderTest {

    /** 箱子界面一行就是 9 格。 */
    private static final int WIDTH = 9;

    private static final List<String> WARNINGS = new ArrayList<String>();

    @BeforeAll
    static void setUp() {
        TestServer.install();
        // 警告出口收进内存，免得打到控制台；部分用例还会读它
        MenuLog.sink(new java.util.function.Consumer<String>() {
            @Override
            public void accept(String message) {
                WARNINGS.add(message);
            }
        });
    }

    // ---------- 用例辅助 ----------

    /**
     * 把布局补齐到 {@value #WIDTH} 格。
     * <p>
     * 「格数」= <b>符号数</b>：反引号对算 1 格、引号外每个非空白字符算 1 格、空白算 0 格
     * （见 {@code LayoutParser}）。补齐只追加尾随空白，不改变符号位置——空白既不占格也不断格。
     */
    private static String[] rows(String... rows) {
        String[] padded = new String[rows.length];
        for (int index = 0; index < rows.length; index++) {
            String row = rows[index];
            StringBuilder text = new StringBuilder(row.length() + WIDTH);
            text.append(row);
            for (int missing = width(row); missing < WIDTH; missing++) {
                text.append(' ');
            }
            padded[index] = text.toString();
        }
        return padded;
    }

    /** 一行的格数：反引号对算 1 格、引号外非空白字符各算 1 格、空白算 0 格。 */
    private static int width(String row) {
        int width = 0;
        for (int index = 0; index < row.length(); index++) {
            char current = row.charAt(index);
            if (current == '`') {
                int end = row.indexOf('`', index + 1);
                if (end < 0) {
                    return width;
                }
                width++;
                index = end;
                continue;
            }
            if (!Character.isWhitespace(current)) {
                width++;
            }
        }
        return width;
    }

    private interface Declare {
        void apply(MenuBuilder b);
    }

    /** 一个模板：布局与声明都由 lambda 给，省掉每个用例写一个类。 */
    private static MenuTemplate template(final String[] layout, final Declare declare) {
        return template(layout, WIDTH, "", declare);
    }

    private static MenuTemplate template(final String[] layout, final int columns,
                                         final String title, final Declare declare) {
        return new MenuTemplate() {
            @Override
            public String[] layout() {
                return layout;
            }

            @Override
            public int columns() {
                return columns;
            }

            @Override
            public String title() {
                return title;
            }

            @Override
            public void declare(MenuBuilder b) {
                declare.apply(b);
            }
        };
    }

    private static MenuDefinition build(MenuTemplate template) {
        MenuBuilder builder = new MenuBuilder();
        template.declare(builder);
        return builder.build(template, null);
    }

    /**
     * 每行格数不是 9 的用例（例如测「一行格数不对」）用这个：
     * 这类布局必须显式把 {@code columns()} 改成实际格数，否则每一行都会因为长度不符而报错。
     */
    private static MenuTemplate narrow(final int columns, final String[] layout, final Declare declare) {
        return template(layout, columns, "", declare);
    }

    /** 一个什么都不返回的空分页数据源。 */
    private static Fetcher emptyPages() {
        return new Fetcher() {
            @Override
            public List<ItemStack> fetch(Player viewer, int page, int perPage) {
                return Collections.emptyList();
            }
        };
    }

    private static ClickHandler recording(final List<String> log, final String name) {
        return new ClickHandler() {
            @Override
            public void handle(Slot slot, MenuContext ctx) {
                log.add(name);
            }
        };
    }

    private static Function<MenuContext, String> constant(final String value) {
        return new Function<MenuContext, String>() {
            @Override
            public String apply(MenuContext ctx) {
                return value;
            }
        };
    }

    // ---------- 校验 ----------

    @Test
    @DisplayName("布局里的符号没在 declare 里声明：抛，并指出未知符号与它首次出现的行列")
    void undeclaredSymbolFails() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                rows("#########", "##`quests`#c#`create`##", "#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').item("border").of(Items.of(Material.GRAY_STAINED_GLASS_PANE));
                        // 故意漏掉 `quests` 与 `create`
                    }
                })));
        assertTrue(failure.getMessage().contains("未声明"), failure.getMessage());
        assertTrue(failure.getMessage().contains("第2行第3列"), failure.getMessage());
    }

    @Test
    @DisplayName("单字符符号与多字符符号互为前缀：构建期抛")
    void prefixConflictFailsAtBuild() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                new String[]{"#########", "c########"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').item("a").of(Items.of(Material.STONE));
                        b.symbol('c').item("b").of(Items.of(Material.DIRT));
                        b.symbol("crate").item("d").of(Items.of(Material.PAPER));
                    }
                })));
        assertTrue(failure.getMessage().contains("前缀冲突"), failure.getMessage());
    }

    @Test
    @DisplayName("同一个符号声明两次：当场抛")
    void duplicateSymbolFails() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                rows("#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').item("a").of(Items.of(Material.STONE));
                        b.symbol('#').item("b").of(Items.of(Material.DIRT));
                    }
                })));
        assertTrue(failure.getMessage().contains("被重复声明"), failure.getMessage());
    }

    @Test
    @DisplayName("符号名含反引号：抛（反引号是布局表的词法边界）")
    void symbolWithTickFails() {
        assertThrows(MenuConfigException.class, () -> build(template(
                rows("#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol("a`b").of(Items.of(Material.STONE));
                    }
                })));
    }

    @Test
    @DisplayName("每页条数 = 分页符号在布局里出现的格数（不是另外声明的数）")
    void perPageFollowsLayout() {
        MenuDefinition two = build(paged("#ll######"));
        assertEquals(2, two.slotsOf("l").size());
        assertTrue(two.stateOf("l").isPaged());
        assertEquals(2, two.stateOf("l").perPage());

        // 同一份 declare()，布局多给一格，容量就跟着涨——不存在「改了布局忘了改常量」
        MenuDefinition four = build(paged("#llll####"));
        assertEquals(4, four.stateOf("l").perPage());
    }

    @Test
    @DisplayName("声明了分页却没进布局：只记一条 warning（那一页一格格子都没有，界面照常开）")
    void pagedSymbolOutsideLayoutOnlyWarns() {
        MenuDefinition definition = build(paged("#########"));
        assertEquals(0, definition.stateOf("l").perPage());
        assertTrue(definition.warnings().stream().anyMatch(w -> w.contains("'l'")),
                definition.warnings().toString());
    }

    /** 一份「# 是石头、l 是分页区」的模板。 */
    private static MenuTemplate paged(String layout) {
        return template(rows(layout), new Declare() {
            @Override
            public void apply(MenuBuilder b) {
                b.symbol('#').of(Items.of(Material.STONE));
                b.symbol('l').pageItems(emptyPages());
            }
        });
    }

    @Test
    @DisplayName("同一个符号同时声明 pageItems 与静态物品：语义冲突，当场抛")
    void pageItemsAndStaticItemConflict() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                rows("l########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.symbol('l').of(Items.of(Material.PAPER)).pageItems(emptyPages());
                    }
                })));
        assertTrue(failure.getMessage().contains("语义冲突"), failure.getMessage());
    }

    @Test
    @DisplayName("占位符名不能为空")
    void placeholderNameMustNotBeEmpty() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                rows("c########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.symbol('c').of(Items.of(Material.PAPER)).ph("", constant("x"));
                    }
                })));
        assertTrue(failure.getMessage().contains("占位符名不能为空"), failure.getMessage());
    }

    @Test
    @DisplayName("ALLOW_MOVE 与 delayedRefresh(true) 不能同时开")
    void moveAndDelayedRefreshConflict() {
        MenuConfigException failure = assertThrows(MenuConfigException.class, () -> build(template(
                rows("#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.interaction(Interaction.ALLOW_MOVE);
                        b.delayedRefresh(true);
                    }
                })));
        assertTrue(failure.getMessage().contains("ALLOW_MOVE"), failure.getMessage());
    }

    @Test
    @DisplayName("fillEmpty 只能设一次")
    void fillEmptyOnce() {
        assertThrows(MenuConfigException.class, () -> build(template(
                rows("#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.fillEmpty(Items.filler());
                        b.fillEmpty(Items.filler());
                    }
                })));
    }

    @Test
    @DisplayName("声明了但没出现在布局里：不抛，记一条警告")
    void declaredButNotInLayoutWarns() {
        MenuDefinition definition = build(template(
                rows("#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                        b.symbol('x').of(Items.of(Material.PAPER));
                    }
                }));
        assertEquals(9, definition.size());
        boolean warned = false;
        for (String warning : definition.warnings()) {
            if (warning.contains("'x'") && warning.contains("没有出现在布局中")) {
                warned = true;
            }
        }
        assertTrue(warned, definition.warnings().toString());
    }

    // ---------- 正常构建 ----------

    @Test
    @DisplayName("正常构建：槽位归属、兜底填充、默认只读")
    void buildsDefinition() {
        MenuDefinition definition = build(template(
                rows("#########", "##`quests`#x#`create`##", "#########"),
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').item("border")
                                .of(Items.of(Material.GRAY_STAINED_GLASS_PANE).name(" "));
                        b.symbol("quests").item("quests_btn")
                                .of(Items.of(Material.WRITABLE_BOOK).name("&f任务列表"));
                        b.symbol('x').item("create_btn")
                                .of(Items.of(Material.WRITABLE_BOOK).name("&f创建任务"));
                        b.symbol("create").item("create_long_btn")
                                .of(Items.of(Material.WRITABLE_BOOK).name("&f创建任务"));
                        b.fillEmpty(Items.filler());
                        b.interaction(Interaction.READ_ONLY);
                    }
                }));

        assertEquals(27, definition.size());
        assertEquals(9, definition.columns());
        assertEquals(Interaction.READ_ONLY, definition.interaction());
        assertFalse(definition.delayedRefresh());
        assertNotNull(definition.filler());

        // 第 2 行：2 格 # + quests + # + x + # + create + 2 格 #
        // → quests 在槽位 11、x 在 13、create 在 15
        assertEquals(1, definition.slotsOf("quests").size());
        assertEquals(11, definition.slotsOf("quests").get(0).index());
        assertEquals("quests", definition.symbolAt(11));
        assertEquals("x", definition.symbolAt(13));
        assertEquals("create", definition.symbolAt(15));

        // 边框占了整张图里除这 3 格以外的所有格
        assertEquals(24, definition.slotsOf("#").size());

        // 按 yml 的键也能找到声明
        assertEquals(definition.stateOf("quests"), definition.stateByConfigKey("quests_btn"));
        assertEquals(definition.stateOf("#"), definition.stateByConfigKey("border"));
    }

    @Test
    @DisplayName("没设 fillEmpty 的空位：是兜底槽，填充物是空气")
    void withoutFillEmpty() {
        // 只声明 1 格：第 0 格被 x 占，第 1 格起没有任何符号 → 都是兜底槽
        MenuDefinition definition = build(narrow(1, new String[]{"x"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('x').of(Items.of(Material.STONE));
                    }
                }));
        // 容器大小仍按「至少 9 格」规范化，因此后 8 格是兜底槽
        assertFalse(definition.isFillerSlot(0));
        assertEquals(Material.AIR, definition.fillerStack().getType());
    }

    @Test
    @DisplayName("点击优先级：按槽位 > 精确键位 > 便捷入口 > 全量兜底")
    void clickPriority() {
        final List<String> log = new ArrayList<String>();
        final ClickHandler fallback = recording(log, "fallback");
        final ClickHandler shiftLeft = recording(log, "shift-left");
        final ClickHandler plainLeft = recording(log, "left");
        final ClickHandler bySlot = recording(log, "slot2");

        MenuDefinition definition = build(narrow(3, new String[]{"#x#"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#')
                                .of(Items.of(Material.STONE))
                                .onClick(fallback)
                                .onLeftClick(plainLeft)
                                .onShiftLeftClick(shiftLeft)
                                .onClick(Collections.singletonMap(Integer.valueOf(2), bySlot));
                        b.symbol('x').of(Items.of(Material.PAPER));
                    }
                }));

        assertEquals(3, definition.size());
        List<Slot> border = definition.slotsOf("#");
        assertEquals(2, border.size());
        Slot first = border.get(0);
        Slot second = border.get(1);
        assertEquals(0, first.index());
        assertEquals(2, second.index());
        // 中间那一格是 x，不是空位
        assertEquals("x", definition.symbolAt(1));

        SymbolState state = definition.stateOf("#");
        // 1) 按槽位号优先，比其它规则都高
        assertEquals(bySlot, state.resolve(second, ClickType.SHIFT_LEFT));
        // 2) Shift+左键有单独注册时走它
        assertEquals(shiftLeft, state.resolve(first, ClickType.SHIFT_LEFT));
        // 3) 普通左键走便捷入口
        assertEquals(plainLeft, state.resolve(first, ClickType.LEFT));
        // 4) 其它键位落到全量兜底
        assertEquals(fallback, state.resolve(first, ClickType.RIGHT));
        assertEquals(fallback, state.resolve(first, ClickType.DROP));
        assertEquals(fallback, state.resolve(first, ClickType.MIDDLE));
    }

    @Test
    @DisplayName("Shift 组合没单独注册时，退化到不带修饰键的那一个")
    void shiftFallsBackToPlain() {
        final ClickHandler plain = recording(new ArrayList<String>(), "left");
        MenuDefinition definition = build(narrow(1, new String[]{"#"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE)).onLeftClick(plain);
                    }
                }));
        SymbolState state = definition.stateOf("#");
        assertEquals(plain, state.resolve(definition.slotsOf("#").get(0), ClickType.SHIFT_LEFT));
    }

    @Test
    @DisplayName("没注册任何回调的符号：resolve 返回 null（点它等同于点到空槽）")
    void noHandlerResolvesToNull() {
        MenuDefinition definition = build(narrow(1, new String[]{"#"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                    }
                }));
        SymbolState state = definition.stateOf("#");
        assertFalse(state.hasClick());
        assertNull(state.resolve(definition.slotsOf("#").get(0), ClickType.LEFT));
    }

    @Test
    @DisplayName("符号有多格时，多个 # 是同一份声明（共用物品定义）")
    void manySlotsShareOneDeclaration() {
        MenuDefinition definition = build(narrow(3, new String[]{"###"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE)).ph("x", constant("1"));
                    }
                }));
        assertEquals(3, definition.slotsOf("#").size());
        assertEquals(3, definition.size());
    }

    @Test
    @DisplayName("分页符号：构建后每页条数就是它在布局里的格数")
    void pagedSymbolBuilds() {
        MenuDefinition definition = build(paged("#ll######"));
        assertEquals(2, definition.slotsOf("l").size());
        assertTrue(definition.stateOf("l").isPaged());
        assertEquals(2, definition.stateOf("l").perPage());
    }

    @Test
    @DisplayName("布局里超过 6 行 / 每行超 9 格：都在构建期抛")
    void badLayoutFailsAtBuild() {
        MenuConfigException tooManyRows = assertThrows(MenuConfigException.class, () -> build(template(
                new String[]{"#########", "#########", "#########",
                        "#########", "#########", "#########", "#########"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                    }
                })));
        assertTrue(tooManyRows.getMessage().contains("超出 1..6"), tooManyRows.getMessage());

        MenuConfigException tooWide = assertThrows(MenuConfigException.class, () -> build(template(
                new String[]{"##########"},
                new Declare() {
                    @Override
                    public void apply(MenuBuilder b) {
                        b.symbol('#').of(Items.of(Material.STONE));
                    }
                })));
        assertTrue(tooWide.getMessage().contains("期望 9 列"), tooWide.getMessage());
    }

    // ---------- 物品与文本 ----------

    @Test
    @DisplayName("Items：材质字符串解析大小写不敏感，认不出来给 null")
    void materialResolution() {
        if (!TestServer.ready()) {
            return;
        }
        assertNotNull(Items.resolve("diamond_sword"));
        assertNotNull(Items.resolve("  DIAMOND_SWORD  "));
        assertNull(Items.resolve("NOT_A_MATERIAL"));
        assertNull(Items.resolve(""));
        assertNull(Items.resolve(null));
        assertThrows(MenuConfigException.class, () -> Items.of("NOT_A_MATERIAL"));
        assertThrows(MenuConfigException.class, () -> Items.of((Material) null));
    }

    @Test
    @DisplayName("未知材质的建议：跨版本改名的提示，以及近似名字的提示")
    void materialSuggestion() {
        if (!TestServer.ready()) {
            return;
        }
        assertTrue(Items.suggest("GRASS").contains("SHORT_GRASS"), Items.suggest("GRASS"));
        assertTrue(Items.suggest("diamond_swor").contains("DIAMOND_SWORD"),
                Items.suggest("diamond_swor"));
        assertEquals("", Items.suggest(""));
        assertEquals("", Items.suggest(null));

        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> Items.of("NOT_A_MATERIAL"));
        assertTrue(failure.getMessage().contains("NOT_A_MATERIAL"), failure.getMessage());
    }

    @Test
    @DisplayName("Placeholders：认不出来的名字原样留着，空值当空串，{0} 不算命名占位符")
    void placeholderRules() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("count", "7");
        values.put("empty", "");
        assertEquals("共 7 个 / {missing} /  / {0}",
                Placeholders.apply("共 {count} 个 / {missing} / {empty} / {0}", values));
        assertEquals(Arrays.asList("count"), Placeholders.names("共 {count} 个 {0} {count}"));
        assertEquals("没有占位符",
                Placeholders.apply("没有占位符", Collections.<String, String>emptyMap()));
        assertEquals("逐行 {x}", Placeholders.apply(Arrays.asList("逐行 {x}"),
                Collections.<String, String>emptyMap()).get(0));
        // 模板为 null / 空：原样返回，不抛
        assertNull(Placeholders.apply((String) null, values));
        assertEquals("", Placeholders.apply("", values));
    }
}
