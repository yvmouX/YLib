package cn.yvmou.ylib.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 占位符替换的契约：参数按<b>字面</b>插入。
 *
 * <p>这条契约锁的是 {@code MessageService} 的 javadoc 承诺——{@code {0}} 位置传入的
 * 内容不参与渲染。踩过的两个坑：</p>
 * <ul>
 *   <li>先替换再渲染，参数里的 {@code <red>} 会被 MiniMessage 当成标签解析掉；</li>
 *   <li>用 {@code replaceAll("\\{\\d+}", "")} 清理未提供的占位符，正则替换的
 *       replacement 会展开 {@code $} 引用，参数含 {@code $1} 时直接抛
 *       {@code IllegalArgumentException: Illegal group reference}。</li>
 * </ul>
 */
class MessageFormatterTest {

    @Test
    @DisplayName("按序替换 {0} {1}")
    void replacesInOrder() {
        assertEquals("你好 Steve，本次 +3",
                MessageFormatter.format("你好 {0}，本次 +{1}", "Steve", 3));
    }

    @Test
    @DisplayName("同一个占位符出现多次，每处都替换")
    void replacesEveryOccurrence() {
        assertEquals("A A A", MessageFormatter.format("{0} {0} {0}", "A"));
    }

    @Test
    @DisplayName("参数含 $ 与 \\ 时按字面插入，不展开正则引用")
    void dollarAndBackslashAreLiteral() {
        assertEquals("价格 $1 起", MessageFormatter.format("价格 {0} 起", "$1"));
        assertEquals("价格 $1 起 ", MessageFormatter.format("价格 {0} 起 {5}", "$1"));
        assertEquals("路径 C:\\x\\y 已创建", MessageFormatter.format("路径 {0} 已创建", "C:\\x\\y"));
    }

    @Test
    @DisplayName("参数含 MiniMessage 标签时原样保留（不参与渲染）")
    void tagsInArgumentsSurvive() {
        assertEquals("§c你好 <red>Steve</red>", MessageFormatter.format("§c你好 {0}", "<red>Steve</red>"));
    }

    @Test
    @DisplayName("参数里的 § 色码保留")
    void sectionCodesInArgumentsSurvive() {
        assertEquals("你好 §cSteve", MessageFormatter.format("你好 {0}", "§cSteve"));
    }

    @Test
    @DisplayName("参数多余的忽略，占位符多余的移除")
    void arityMismatch() {
        assertEquals("你好 Steve", MessageFormatter.format("你好 {0}", "Steve", "多余"));
        assertEquals("你好 Steve！", MessageFormatter.format("你好 {0}{2}！", "Steve", "x"));
        assertEquals("你好 ！", MessageFormatter.format("你好 {2}！", "Steve"));
    }

    @Test
    @DisplayName("不合法或越界的花括号不是占位符")
    void malformedBracesAreKept() {
        assertEquals("{abc} 与 {} 原样保留",
                MessageFormatter.format("{abc} 与 {} 原样保留", "x"));
        assertEquals("单个 { 原样保留", MessageFormatter.format("单个 { 原样保留", "x"));
        // 超长数字下标按越界处理（移除），不能因为解析成 int 失败而抛异常
        assertEquals("你好 ！", MessageFormatter.format("你好 {99999999999999}！", "Steve"));
    }

    @Test
    @DisplayName("前导零按数字解析：{01} 取的是第 2 个参数")
    void leadingZeroIsNumeric() {
        // 与旧的 replace("{1}", ...) 略有差异：旧写法把 {01} 拼成 "{0" + "1}"，
        // 替换后留下 "1}"。这里按数字语义处理，语言文件里不写前导零就不受影响。
        assertEquals("B", MessageFormatter.format("{01}", "A", "B"));
        assertEquals("A", MessageFormatter.format("{00}", "A", "B"));
    }

    @Test
    @DisplayName("没有参数时原样返回，花括号不被动")
    void noArgsIsNoOp() {
        assertEquals("你好 {0}", MessageFormatter.format("你好 {0}"));
    }

    @Test
    @DisplayName("空串与 null 参数不抛异常")
    void edgeCases() {
        assertEquals("", MessageFormatter.format(""));
        assertEquals("你好 null", MessageFormatter.format("你好 {0}", (Object) null));
        assertDoesNotThrow(() -> MessageFormatter.format("{0}", "$1"));
    }

    @Test
    @DisplayName("时间等中文正文里的数字不受影响")
    void digitsInBodySurvive() {
        assertEquals("剩余 3 天 12 小时", MessageFormatter.format("剩余 3 天 12 小时"));
        assertEquals("剩余 3 天，来自 Steve", MessageFormatter.format("剩余 3 天，来自 {0}", "Steve"));
    }
}
