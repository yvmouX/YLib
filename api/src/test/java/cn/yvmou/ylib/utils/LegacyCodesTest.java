package cn.yvmou.ylib.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 传统色码的转换与剥离：{@code &} ↔ {@code §}、十六进制、{@code &&} 转义、非法码保留。
 *
 * <h2>为什么这些用例值得存在</h2>
 * 这套转换原先有三份实现（logger 一份没有十六进制、text 一份输出 MiniMessage 标签、
 * menu 一份支持 {@code &&} 转义），合并之后必须把三处**各自依赖的行为**都钉住，
 * 否则合并就是静默的行为变更：
 * <ul>
 *   <li>logger 依赖「认不出来就原样保留」，不然日志正文里的 {@code &} 会被吃掉；</li>
 *   <li>menu 依赖 {@code &&} 转义与十六进制，因为物品名/标题里有人写 {@code &#RRGGBB}；</li>
 *   <li>两者都依赖「重复调用幂等」——同一串经过两次转换不该变形。</li>
 * </ul>
 */
class LegacyCodesTest {

    private static final char S = LegacyCodes.section();

    @Test
    @DisplayName("基本色码：& → §")
    void basicCodes() {
        assertEquals(S + "aHello", LegacyCodes.toSection("&aHello"));
        assertEquals(S + "c" + S + "l粗体", LegacyCodes.toSection("&c&l粗体"));
        assertEquals(S + "r重置", LegacyCodes.toSection("&r重置"));
    }

    @Test
    @DisplayName("大小写都认：&A 与 &a 等价，且输出统一为小写")
    void caseInsensitive() {
        assertEquals(S + "a", LegacyCodes.toSection("&A"));
        assertEquals(S + "f", LegacyCodes.toSection("&F"));
        // 归一化不是为了好看：不归一的话 &A 与 &a 会产出不同字节，
        // 「同一份文案经不同路径得到同一结果」这条幂等断言就会失败
        assertEquals(LegacyCodes.toSection("&a"), LegacyCodes.toSection("&A"));
    }

    @Test
    @DisplayName("十六进制 &#RRGGBB → §x§R§R§G§G§B§B")
    void hexHashForm() {
        assertEquals(S + "x" + S + "f" + S + "f" + S + "8" + S + "8" + S + "0" + S + "0" + "橙",
                LegacyCodes.toSection("&#ff8800橙"));
        // 大小写混写的十六进制也认
        assertEquals(S + "x" + S + "A" + S + "b" + S + "C" + S + "d" + S + "E" + S + "f",
                LegacyCodes.toSection("&#AbCdEf"));
    }

    @Test
    @DisplayName("已经是 §x§R… 的写法原样放行，不被当成普通字符")
    void alreadySectionHex() {
        String already = S + "x" + S + "f" + S + "f" + S + "0" + S + "0" + S + "0" + S + "0";
        assertEquals(already, LegacyCodes.toSection(already));
    }

    @Test
    @DisplayName("&& 转义成一个字面量 &")
    void escapedAmpersand() {
        assertEquals("A&B", LegacyCodes.toSection("A&&B"));
        // 转义后的 & 后面跟字母，不该被当成色码
        assertEquals("A&aB", LegacyCodes.toSection("A&&aB"));
    }

    @Test
    @DisplayName("非法码原样保留：& 后面跟普通文字时不吃掉字符")
    void illegalCodeKept() {
        // 注意 &B 是**合法**色码（蓝色 &b 的大写写法），因此真正的非法码要用字母表外的字符
        assertEquals(S + "b", LegacyCodes.toSection("&B"), "&B 是合法色码，统一成小写");
        assertEquals("A&Z", LegacyCodes.toSection("A&Z"));
        assertEquals("Tom & Jerry", LegacyCodes.toSection("Tom & Jerry"));
        assertEquals("结尾&", LegacyCodes.toSection("结尾&"));
        // &# 后面不是合法十六进制
        assertEquals("&#ZZZZZZ", LegacyCodes.toSection("&#ZZZZZZ"));
    }

    @Test
    @DisplayName("null / 空串原样返回")
    void nullAndEmpty() {
        assertNull(LegacyCodes.toSection(null));
        assertEquals("", LegacyCodes.toSection(""));
        assertNull(LegacyCodes.strip(null));
        assertEquals("", LegacyCodes.strip(""));
    }

    @Test
    @DisplayName("幂等：同一串转两次不变形")
    void idempotent() {
        String once = LegacyCodes.toSection("&a共 &e7 &7个 && &#ff8800橙");
        assertEquals(once, LegacyCodes.toSection(once));
    }

    @Test
    @DisplayName("strip：去掉全部色码只留正文")
    void stripRemovesCodes() {
        assertEquals("Hello粗体", LegacyCodes.strip("&aHello&l粗体"));
        assertEquals("Hello", LegacyCodes.strip(S + "aHello"));
        assertEquals("橙", LegacyCodes.strip("&#ff8800橙"));
        // && 还原成一个 &，而不是被当成色码删掉
        assertEquals("A&B", LegacyCodes.strip("A&&B"));
        // 非法码原样保留：**两个字符都要留**（只留 & 会把后面的字符吞掉，这正是合并三份实现时踩到的坑）
        assertEquals("A&Z", LegacyCodes.strip("A&Z"));
        assertEquals("Tom & Jerry", LegacyCodes.strip("Tom & Jerry"));
        assertEquals("结尾&", LegacyCodes.strip("结尾&"));
    }

    @Test
    @DisplayName("strip 与 toSection 对「非法码」判定一致：都不吞字符")
    void stripAgreesWithToSection() {
        for (String raw : new String[]{"A&Z", "Tom & Jerry", "结尾&", "&#ZZZZZZ", "a&z&c"}) {
            assertEquals(LegacyCodes.strip(raw), LegacyCodes.strip(LegacyCodes.toSection(raw)),
                    "两条路径对 [" + raw + "] 的结果不一致");
        }
    }

    @Test
    @DisplayName("strip 与 toSection 组合：先转换再剥离，得到纯文本")
    void stripAfterConvert() {
        String text = "&a你好 &e世界 &7(&f7&7/&f10&7) &#ff8800橙";
        assertEquals("你好 世界 (7/10) 橙", LegacyCodes.strip(LegacyCodes.toSection(text)));
    }

    @Test
    @DisplayName("isCode 只认 0-9a-f / k-o / r")
    void isCode() {
        assertTrue(LegacyCodes.isCode('a'));
        assertTrue(LegacyCodes.isCode('F'));
        assertTrue(LegacyCodes.isCode('k'));
        assertTrue(LegacyCodes.isCode('o'));
        assertTrue(LegacyCodes.isCode('r'));
        assertFalse(LegacyCodes.isCode('g'));
        assertFalse(LegacyCodes.isCode('p'));
        assertFalse(LegacyCodes.isCode('z'));
        assertFalse(LegacyCodes.isCode('#'));
    }
}
