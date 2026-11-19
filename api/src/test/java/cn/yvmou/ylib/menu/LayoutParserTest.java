package cn.yvmou.ylib.menu;

import cn.yvmou.ylib.menu.internal.LayoutParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 布局解析：{@code 文档/菜单.md}「布局表」。
 *
 * <h2>「格数」= 符号数（token）</h2>
 * 这是本节唯一的度量口径，也是「每行 9 格」这句话的实现含义：
 * <ul>
 *   <li>裸写的每个非空白字符算 <b>1</b> 格；</li>
 *   <li>一个反引号对（{@code `quests`}）算 <b>1</b> 格——引号内的名字被收集成同一个符号；</li>
 *   <li>空白算 <b>0</b> 格，只用于源码对齐，且不打断符号的连续性。</li>
 * </ul>
 * 于是 {@code "##`quests`#c#`create`##"} 是 2 + 1 + 1 + 1 + 1 + 1 + 2 = <b>9</b> 格，
 * 正好放满一行；而去掉反引号的 {@code "##quests#create##"} 是 <b>17</b> 格——每个字母都成了
 * 独立的单字符符号。这就是「反引号不是装饰，是词法边界」的实际含义。
 * <p>
 * 这一层是整套 API 里唯一「写错一个字就整张图错位」的地方，因此每条规则都变成断言。
 */
class LayoutParserTest {

    @BeforeAll
    static void setUp() {
        TestServer.install();
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

    // ---------- 基本映射 ----------

    @Test
    @DisplayName("单字符符号：一个字符一格，槽位号是行主序")
    void singleCharSymbols() {
        LayoutParser.Result result = LayoutParser.parse(new String[]{"ab", "cd"}, 2);
        assertEquals(4, result.size());
        assertEquals(0, result.slotsBySymbol().get("a").get(0).index());
        assertEquals(2, result.slotsBySymbol().get("c").get(0).index());
        assertEquals(3, result.slotsBySymbol().get("d").get(0).index());
        assertEquals(1, result.slotsBySymbol().get("d").get(0).row());
        assertEquals(1, result.slotsBySymbol().get("d").get(0).col());
    }

    @Test
    @DisplayName("同一个符号可以落在 N 个槽位上（边框的标准用法）")
    void sameSymbolCanOccupyManySlots() {
        LayoutParser.Result result = LayoutParser.parse(new String[]{"#########"}, 9);
        assertEquals(9, result.slotsBySymbol().get("#").size());
    }

    @Test
    @DisplayName("文档里的 ##`quests`#c#`create`## 正好 9 格")
    void documentExampleIsNineSlots() {
        assertEquals(9, width("##`quests`#c#`create`##"));
        LayoutParser.Result result = LayoutParser.parse(
                new String[]{"#########", "##`quests`#c#`create`##", "#########"}, 9);
        assertEquals(27, result.size());
        assertEquals("quests", result.symbolAt(11));
        assertEquals("c", result.symbolAt(13));
        assertEquals("create", result.symbolAt(15));
        assertEquals(24, result.slotsBySymbol().get("#").size());
    }

    @Test
    @DisplayName("反引号对内字符不各自占格：`quests` 只占 1 格")
    void tickedNameTakesExactlyOneSlot() {
        LayoutParser.Result result = LayoutParser.parse(new String[]{"##`quests`"}, 3);
        assertEquals(3, result.size());
        assertEquals("#", result.symbolAt(0));
        assertEquals("#", result.symbolAt(1));
        assertEquals("quests", result.symbolAt(2));
        assertEquals(1, result.slotsBySymbol().get("quests").size());
    }

    @Test
    @DisplayName("去掉反引号后每个字符各算一格：##quests## 是 10 格")
    void withoutTicksEveryCharIsASymbol() {
        assertEquals(10, width("##quests##"));
        LayoutParser.Result result = LayoutParser.parse(new String[]{"##quests##"}, 10);
        assertEquals(10, result.size());
        assertEquals("q", result.symbolAt(2));
        assertEquals(1, result.slotsBySymbol().get("q").size());
        // quests 里有两个 s，因此它占 2 格
        assertEquals(2, result.slotsBySymbol().get("s").size());
    }

    @Test
    @DisplayName("两个多字符符号相邻必须留分隔：`q`#`c`")
    void adjacentTickedSymbolsNeedSeparator() {
        // 2(##) + 1(`q`) + 1(#) + 1(`c`) + 2(##) = 7 格
        LayoutParser.Result result = LayoutParser.parse(new String[]{"##`q`#`c`##"}, 7);
        assertEquals(7, result.size());
        assertEquals("#", result.symbolAt(0));
        assertEquals("q", result.symbolAt(2));
        assertEquals("c", result.symbolAt(4));
        // 分隔用的那个 # 加上首尾的 2+2 个，一共 5 格
        assertEquals(5, result.slotsBySymbol().get("#").size());
    }

    // ---------- 空白 ----------

    @Test
    @DisplayName("空白不占格也不断格：写多宽的空隙都不影响格号")
    void whitespaceDoesNotConsumeSlots() {
        LayoutParser.Result packed = LayoutParser.parse(new String[]{"#c#c#c#c#"}, 9);
        LayoutParser.Result spaced = LayoutParser.parse(new String[]{"#  c  #  c  #  c  #  c  #"}, 9);
        assertEquals(9, packed.size());
        assertEquals(9, spaced.size());
        for (int index = 0; index < 9; index++) {
            assertEquals(packed.symbolAt(index), spaced.symbolAt(index), "第 " + index + " 格");
        }
        assertEquals("c", spaced.symbolAt(1));
    }

    @Test
    @DisplayName("空格只是空隙：符号挨着排，不会在中间留出空槽")
    void whitespaceDoesNotLeaveGaps() {
        // 两个符号之间有 5 个空格，但它们仍然落在第 0 与第 1 格
        LayoutParser.Result result = LayoutParser.parse(new String[]{"#     c"}, 2);
        assertEquals(2, result.size());
        assertEquals("#", result.symbolAt(0));
        assertEquals("c", result.symbolAt(1));
        assertNull(result.symbolAt(2));
    }

    // ---------- 报错 ----------

    @Test
    @DisplayName("每行格数不等于 columns 就报错，并指出是第几行、实际几格")
    void rowWidthMustMatchColumns() {
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> LayoutParser.parse(new String[]{"#########", "##"}, 9));
        assertTrue(failure.getMessage().contains("第2行"), failure.getMessage());
        assertTrue(failure.getMessage().contains("共 2 列"), failure.getMessage());
        assertTrue(failure.getMessage().contains("期望 9 列"), failure.getMessage());
    }

    @Test
    @DisplayName("一行超过 columns：报错而不是把多出来的格子丢掉")
    void tooManyColumnsInOneRow() {
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> LayoutParser.parse(new String[]{"##########"}, 9));
        assertTrue(failure.getMessage().contains("期望 9 列"), failure.getMessage());
    }

    @Test
    @DisplayName("反引号未闭合：报错并指出起始行列")
    void unclosedTick() {
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> LayoutParser.parse(new String[]{"##`quests##"}, 9));
        assertTrue(failure.getMessage().contains("反引号未闭合"), failure.getMessage());
        assertTrue(failure.getMessage().contains("第1行第3列"), failure.getMessage());
    }

    @Test
    @DisplayName("空的反引号对：报错")
    void emptyTickPair() {
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> LayoutParser.parse(new String[]{"##``#####"}, 9));
        assertTrue(failure.getMessage().contains("空的反引号对"), failure.getMessage());
    }

    @Test
    @DisplayName("空布局 / 超过 6 行：都当场抛")
    void emptyAndTooManyRows() {
        assertThrows(MenuConfigException.class, () -> LayoutParser.parse(new String[0], 9));
        assertThrows(MenuConfigException.class, () -> LayoutParser.parse(null, 9));
        assertThrows(MenuConfigException.class, () -> LayoutParser.parse(
                new String[]{"#", "#", "#", "#", "#", "#", "#"}, 9));
    }

    @Test
    @DisplayName("单字符符号与多字符符号互为前缀：报错（裸写时无法区分）")
    void prefixConflict() {
        MenuConfigException failure = assertThrows(MenuConfigException.class,
                () -> LayoutParser.checkPrefixConflict(Arrays.asList("q", "quests")));
        assertTrue(failure.getMessage().contains("前缀冲突"), failure.getMessage());
        // 不互为前缀就不该报
        LayoutParser.checkPrefixConflict(Arrays.asList("#", "quests"));
    }

    @Test
    @DisplayName("未知符号的建议：把单字符当成多字符符号的首字母时提示它")
    void suggestsTickedSymbol() {
        assertEquals("（是否想写 `quests`?）",
                LayoutParser.suggest("q", Arrays.asList("quests", "create")));
        assertEquals("", LayoutParser.suggest("z", Arrays.asList("quests")));
    }

    @Test
    @DisplayName("中文占 1 格；emoji 是两个 char，本库按 UTF-16 单元切，不做码点归并")
    void utf16UnitsCountAsColumns() {
        // 门：1 个 char；🔥：代理对，2 个 char；#：1 个 char → 共 4 格
        LayoutParser.Result result = LayoutParser.parse(new String[]{"门\uD83D\uDD25#"}, 4);
        assertEquals(4, result.size());
        assertEquals("门", result.symbolAt(0));
        assertEquals("\uD83D", result.symbolAt(1));
        assertEquals("\uDD25", result.symbolAt(2));
        assertEquals("#", result.symbolAt(3));
    }

    @Test
    @DisplayName("多字符符号跨行重复出现：每个槽位都记进同一份声明")
    void multiCharSymbolAcrossRows() {
        // `p` 占 1 格，加上 4 个 # = 5 格
        LayoutParser.Result result = LayoutParser.parse(
                new String[]{"`p`####", "####`p`"}, 5);
        List<Slot> prev = result.slotsBySymbol().get("p");
        assertEquals(2, prev.size());
        assertEquals(0, prev.get(0).index());
        assertEquals(9, prev.get(1).index());
        assertEquals(1, prev.get(1).row());
        assertEquals(4, prev.get(1).col());
    }

    @Test
    @DisplayName("跨行时按行号算槽位")
    void slotIndicesSpanRows() {
        LayoutParser.Result result = LayoutParser.parse(new String[]{"#", "c"}, 1);
        assertEquals(2, result.size());
        assertEquals("#", result.symbolAt(0));
        assertEquals("c", result.symbolAt(1));
        assertEquals(1, result.slotsBySymbol().get("c").get(0).row());
        assertEquals(0, result.slotsBySymbol().get("c").get(0).col());
    }

    @Test
    @DisplayName("槽位 → 符号：没写到的格子是 null（由 fillEmpty 兜底）")
    void unwrittenSlotsAreNull() {
        LayoutParser.Result result = LayoutParser.parse(new String[]{"#"}, 1);
        assertEquals("#", result.symbolAt(0));
        assertNull(result.symbolAt(1));
        assertNull(result.symbolAt(-1));
    }
}
