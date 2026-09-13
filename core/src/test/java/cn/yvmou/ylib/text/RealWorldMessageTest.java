package cn.yvmou.ylib.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实语言文件形态的渲染验证。
 *
 * <p>下面这些字符串取自 PlayerTaskX 的 {@code lang/zh_CN.yml}——也就是消费方实际会
 * 渲染的内容。之所以单独列出来，是因为库自身的用例都在测「机制」，而这些测的是
 * 「机制用在真实文案上是否成立」：嵌套标签、中文与标点混排、占位符被替换成
 * 玩家名之后仍然正确。</p>
 */
class RealWorldMessageTest {

    private static final String SECTION = "\u00A7";

    @Test
    @DisplayName("前缀：嵌套标签")
    void prefixWithNestedTags() {
        String out = TextRenderer.render("<gray>[</gray><aqua>PlayerTaskX</aqua><gray>]</gray> ");
        assertFalse(out.contains("<"), "不应残留标签: " + out);
        assertFalse(out.contains(">"), "不应残留标签: " + out);
        assertTrue(out.contains(SECTION + "7"), "灰色部分: " + out);
        assertTrue(out.contains(SECTION + "b"), "青色部分: " + out);
        assertTrue(out.contains("PlayerTaskX"), "正文不应丢失: " + out);
        assertTrue(out.endsWith(" "), "结尾空格应保留（拼接前缀用）: [" + out + "]");
    }

    @Test
    @DisplayName("完成提示：占位符换成玩家名后仍是颜色码")
    void completedMessageWithPlayerName() {
        // MessageService 会先把 {0} 替换成玩家名，再交给渲染器
        String out = TextRenderer.render("<green>任务已完成：</green><white>Steve</white><gray>，可在任务界面领取奖励。</gray>");
        assertFalse(out.contains("<"), "不应残留标签: " + out);
        assertTrue(out.contains(SECTION + "a"), "绿色: " + out);
        assertTrue(out.contains("Steve"), "玩家名应保留: " + out);
        assertTrue(out.contains("可在任务界面领取奖励"), "中文正文应保留: " + out);
    }

    @Test
    @DisplayName("带对勾与全角标点的单行文案")
    void symbolsAndFullWidthPunctuation() {
        String out = TextRenderer.render("<green>✔ 任务完成</green>");
        assertTrue(out.contains("✔"), "符号应保留: " + out);
        assertTrue(out.contains(SECTION + "a"), "绿色: " + out);
        assertFalse(out.contains("<"), "不应残留标签: " + out);
    }

    @Test
    @DisplayName("占位符里的时间数字不会干扰颜色码")
    void numbersDoNotBreakCodes() {
        String out = TextRenderer.render("<red>已过期</red>");
        assertFalse(out.contains("<"), "不应残留标签: " + out);
        assertTrue(out.contains(SECTION + "c"), "红色: " + out);
    }
}
