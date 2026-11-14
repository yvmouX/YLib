package cn.yvmou.ylib.text;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文本渲染的约束测试。
 *
 * <p>这里钉住的都是「同一条字符串里混用多种写法」的行为。它们之所以必须集中测，
 * 是因为单独一种写法都很容易正确，难的是混排时谁也不破坏谁——而这正是消费方
 * 唯一会踩的形态。</p>
 */
class TextRendererTest {

    private static final String SECTION = "\u00A7";

    @Test
    @DisplayName("& 颜色码被转换，不残留字面量")
    void ampersandIsTranslated() {
        String out = TextRenderer.render("&a绿色 &l加粗");
        assertTrue(out.contains(SECTION + "a"), "应含 §a: " + out);
        assertTrue(out.contains(SECTION + "l"), "应含 §l: " + out);
        assertFalse(out.contains("&"), "不应残留 & 字面量: " + out);
    }

    @Test
    @DisplayName("MiniMessage 标签被解析，不残留标签原文")
    void miniMessageTagsAreParsed() {
        String out = TextRenderer.render("<green>绿色</green> <bold>加粗</bold>");
        assertTrue(out.contains(SECTION + "a"), "应含 §a: " + out);
        assertTrue(out.contains(SECTION + "l"), "应含 §l: " + out);
        assertFalse(out.contains("<"), "不应残留标签: " + out);
    }

    @Test
    @DisplayName("标签与 § 码混排时都能正确处理（这正是以前做错的地方）")
    void tagsAndSectionCodesMix() {
        // MiniMessage 遇到 § 会整串放弃解析，所以必须先归一化再渲染
        String out = TextRenderer.render("<yellow>任务" + SECTION + "8 | " + SECTION + "f进度");
        assertTrue(out.contains(SECTION + "e"), "标签应变黄: " + out);
        assertTrue(out.contains(SECTION + "8"), "原有 §8 应保留: " + out);
        assertTrue(out.contains(SECTION + "f"), "原有 §f 应保留: " + out);
        assertFalse(out.contains("<"), "不应残留标签: " + out);
    }

    @Test
    @DisplayName("标签与 & 码混排时都能正确处理")
    void tagsAndAmpersandMix() {
        String out = TextRenderer.render("<yellow>任务</yellow> &8| &f进度");
        assertTrue(out.contains(SECTION + "e"), "标签应变黄: " + out);
        assertTrue(out.contains(SECTION + "8"), "&8 应变 §8: " + out);
        assertTrue(out.contains(SECTION + "f"), "&f 应变 §f: " + out);
    }

    @Test
    @DisplayName("十六进制颜色：&#RRGGBB 与 §x§R§R§G§G§B§B 都支持")
    void hexColorsAreSupported() {
        String fromAmpersand = TextRenderer.render("&#ff8800橙色");
        assertFalse(fromAmpersand.contains("<"), "不应残留标签: " + fromAmpersand);
        // legacySection 序列化十六进制时会写成 §x§f§f§8§8§0§0
        assertTrue(fromAmpersand.contains(SECTION + "x"), "应输出 §x 十六进制形式: " + fromAmpersand);

        String fromSection = TextRenderer.render(SECTION + "x" + SECTION + "f" + SECTION + "f"
                + SECTION + "8" + SECTION + "8" + SECTION + "0" + SECTION + "0" + "橙色");
        assertEquals(fromAmpersand, fromSection, "两种十六进制写法应渲染成同一结果");
    }

    @Test
    @DisplayName("非法标签退化为纯文本，不把标签抛给玩家")
    void malformedTagFallsBackToPlainText() {
        // 未闭合/未知标签：宁可显示原文，也不该抛异常或丢掉内容
        String out = TextRenderer.render("生命值 <3 点");
        assertFalse(out.isEmpty());
        assertTrue(TextRenderer.strip(out).contains("生命值"), "正文不应丢失: " + out);
    }

    @Test
    @DisplayName("§ 后面跟普通字符时原样保留，不吞正文")
    void invalidCodeKeepsText() {
        String out = TextRenderer.normalize("价格 100" + SECTION + "z 元");
        assertTrue(out.contains("100"), "数字应保留: " + out);
        assertTrue(out.contains("元"), "中文应保留: " + out);
    }

    @Test
    @DisplayName("strip 去掉全部格式，仅保留纯文本")
    void stripRemovesFormatting() {
        assertEquals("绿色加粗", TextRenderer.strip("&a绿色&l加粗"));
        assertEquals("任务进度", TextRenderer.strip("<yellow>任务</yellow><gray>进度"));
    }

    @Test
    @DisplayName("渲染是幂等的：已渲染的 § 码再渲染一次也不变形")
    void renderIsIdempotent() {
        String once = TextRenderer.render("<yellow>任务 &8| &f50%");
        String twice = TextRenderer.render(once);
        assertEquals(once, twice, "二次渲染不应改变结果");
    }

    @Test
    @DisplayName("斜体被统一关闭（原版会把自定义名称渲染成斜体）")
    void italicIsDisabled() {
        Component component = TextRenderer.parse("普通文本");
        assertEquals(net.kyori.adventure.text.format.TextDecoration.State.FALSE,
                component.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC),
                "应显式关闭斜体");
    }

    @Test
    @DisplayName("空值安全")
    void nullSafe() {
        assertEquals("", TextRenderer.render(null));
        assertEquals("", TextRenderer.render(""));
        assertEquals("", TextRenderer.strip(null));
        assertEquals("", TextRenderer.normalize(null));
        assertEquals(Component.empty(), TextRenderer.parse(null));
        assertTrue(TextRenderer.isBlank("   "));
    }
}
