package cn.yvmou.ylib.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 命名占位符的三件事：取值表（{@link Placeholders#vars}）、替换（{@link Placeholders#apply}）、
 * 扫模板里用到的名字（{@link Placeholders#names}）。
 * <p>
 * 几条刻意的行为：
 * <ul>
 *   <li><b>认不出来的名字原样留着</b>——界面上的 {@code {foo}} 就是「这里写错了」的提示，悄悄删掉只会让人以为配置生效了；</li>
 *   <li><b>不动 {@code {0}} 这类数字写法</b>——那是语言文件的位置占位符，两套混用会互相吃掉，所以各管一段
 *       （扫名字时也不把它当名字，否则对账会报「代码没给 0」）；</li>
 *   <li><b>没闭合的花括号按普通文本处理</b>——不吞掉后面的内容。</li>
 * </ul>
 */
class PlaceholdersTest {

    // ---------- 取值表 ----------

    @Test
    @DisplayName("成对取值：int / 枚举 / 任何对象都行，自动转字符串")
    void pairsBecomeStrings() {
        Map<String, String> vars = Placeholders.vars("count", 3, "quest", "mining", "enabled", true);

        assertEquals("3", vars.get("count"));
        assertEquals("mining", vars.get("quest"));
        assertEquals("true", vars.get("enabled"));
        assertEquals(3, vars.size());
    }

    @Test
    @DisplayName("一个都不给就是空表（空表在替换里等于不用替换）")
    void emptyIsEmpty() {
        assertTrue(Placeholders.vars().isEmpty());
        assertTrue(Placeholders.empty().isEmpty());
        assertTrue(Placeholders.vars((Object[]) null).isEmpty());
        assertEquals("没事发生", Placeholders.apply("没事发生", Placeholders.vars()));
    }

    @Test
    @DisplayName("null 值转空串：占位符位置留空，而不是显示 null")
    void nullValueBecomesEmptyString() {
        assertEquals("任务: ", Placeholders.apply("任务: {id}", Placeholders.vars("id", null)));
    }

    @Test
    @DisplayName("同名后者赢（调用方拼接时覆盖），顺序按给入顺序")
    void lastOneWins() {
        Map<String, String> vars = Placeholders.vars("a", 1, "b", 2, "a", 9);

        assertEquals("9", vars.get("a"));
        assertEquals("a,b", String.join(",", vars.keySet()));
    }

    @Test
    @DisplayName("写少一个值当场抛并说明格式：错位的文案最难查")
    void oddArgumentsThrow() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Placeholders.vars("count", 3, "quest"));
        assertTrue(failure.getMessage().contains("成对"), failure.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Placeholders.vars(null, 1));
    }

    // ---------- 替换 ----------

    @Test
    @DisplayName("按名字替换；同一个名字出现几次就替换几次")
    void replacesEveryOccurrence() {
        assertEquals("挖矿 / 挖矿", Placeholders.apply("{id} / {id}", Map.of("id", "挖矿")));
    }

    @Test
    @DisplayName("认不出来的名字原样保留（写错的占位符要在界面上看得见）")
    void unknownNamesAreKept() {
        assertEquals("共 {count} 个", Placeholders.apply("共 {count} 个", Map.of("id", "挖矿")));
    }

    @Test
    @DisplayName("数字写法不受影响：那是语言文件的占位符，由 MessageService 处理")
    void digitPlaceholdersAreNotTouched() {
        assertEquals("{0} 个任务", Placeholders.apply("{0} 个任务", Map.of("id", "挖矿")));
    }

    @Test
    @DisplayName("没有取值表、或没有花括号时原样返回")
    void nothingToDoReturnsTemplate() {
        assertEquals("{id}", Placeholders.apply("{id}", Map.of()));
        assertEquals("任务列表", Placeholders.apply("任务列表", Map.of("id", "挖矿")));
        assertNull(Placeholders.apply((String) null, Map.of("id", "挖矿")));
    }

    @Test
    @DisplayName("没闭合的花括号按普通文本处理（不吞掉后面的内容）")
    void unclosedBraceIsPlainText() {
        assertEquals("共 {count 个", Placeholders.apply("共 {count 个", Map.of("count", "3")));
        assertEquals("共 }count{ 个", Placeholders.apply("共 }count{ 个", Map.of("count", "3")));
    }

    @Test
    @DisplayName("取值为空串时替换成空串（那是「这里就是空的」，不是「没写」）")
    void emptyValueIsSubstituted() {
        assertEquals("挖矿:", Placeholders.apply("{id}:{suffix}", Map.of("id", "挖矿", "suffix", "")));
    }

    @Test
    @DisplayName("lore 逐行替换；空行照旧保留（空行是分段，不是漏写）")
    void replacesLineByLine() {
        List<String> lines = Placeholders.apply(Arrays.asList("&8{id}", "", "&7类型: &f{type}"),
                Map.of("id", "mining", "type", "DAILY"));

        assertEquals(Arrays.asList("&8mining", "", "&7类型: &fDAILY"), lines);
    }

    // ---------- 扫名字（契约对账的输入） ----------

    @Test
    @DisplayName("扫出模板里用到的名字，按出现顺序去重")
    void namesInOrder() {
        assertEquals(setOf("count", "quest"),
                Placeholders.names("共 {count} 个任务，{quest} 里最长的一个，又是 {count}"));
    }

    @Test
    @DisplayName("数字写法不算名字（那是 MessageService 的活），未闭合的花括号也不算")
    void numericAndUnclosedAreNotNames() {
        assertTrue(Placeholders.names("{0} 个任务").isEmpty());
        assertTrue(Placeholders.names("共 {count 个").isEmpty());
        assertTrue(Placeholders.names("{}").isEmpty(), "空名字不算");
        assertTrue(Placeholders.names((String) null).isEmpty(), "null 模板给空集合（与 apply 一样要写清是哪个重载）");
        assertTrue(Placeholders.names("").isEmpty());
    }

    @Test
    @DisplayName("一组模板取并集（lore 用）")
    void namesOfLines() {
        assertEquals(setOf("id", "type", "page"),
                Placeholders.names(Arrays.asList("&8{id}", "&7类型: &f{type}", "&7分页 {page}")));
    }

    private static java.util.Set<String> setOf(String... names) {
        return new LinkedHashSet<String>(Arrays.asList(names));
    }
}
