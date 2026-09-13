package cn.yvmou.ylib.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * 文本渲染：MiniMessage 为主，兼容传统 {@code &} 与 {@code §} 颜色码。
 *
 * <h2>为什么三种写法要一起处理</h2>
 * MiniMessage 标签、{@code &} 码、{@code §} 码在配置文件里都常见，而它们<b>无法各走各路</b>：
 * <ul>
 *   <li>MiniMessage 遇到 {@code §} 会抛
 *       {@code ParsingExceptionImpl: Legacy formatting codes have been detected}，
 *       且是<b>整串</b>放弃解析——不是只跳过那一段；</li>
 *   <li>{@code LegacyComponentSerializer} 的 round-trip 会把 {@code §} 码原样写回
 *       （解析时识别为样式、序列化时又写出来），所以不能靠它「清洗」残留的颜色码；</li>
 *   <li>它对普通文本里的 {@code <yellow>} 一律当字面量。</li>
 * </ul>
 * 结论：只要字符串里混有 {@code §} 码，单独任何一条路径都处理不好。因此这里统一把
 * {@code &} / {@code §} 码先翻译成等价的 MiniMessage 标签，再用 MiniMessage 渲染一次，
 * 三种写法便能任意混排，输出始终一致。
 *
 * <h2>输出格式</h2>
 * 默认输出传统 {@code §} 色码字符串。这是刻意的选择：Bukkit 的
 * {@code sendMessage(String)} / {@code sendTitle(String)} / {@code sendActionBar(String)}
 * 这类旧式接口<b>都只认 {@code §} 码</b>（实测 Paper 的 {@code sendActionBar(String)}
 * 不解析 MiniMessage，会把标签原样显示给玩家），而需要 Adventure 原生的场景
 * 可以用 {@link #parse(String)} 拿 {@link Component}。
 *
 * <h2>失败时不把标签泄漏给玩家</h2>
 * 标签写法非法（未闭合、未知标签）时退化为纯文本，而不是把 {@code <red>} 这样的
 * 原文显示出来——配置写错不该让玩家看到内部语法。
 */
public final class TextRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /**
     * 输出 {@code §} 形式。
     * <p>
     * {@code hexColors()} 不可省略：默认的 {@code legacySection()} 遇到十六进制颜色会
     * <b>静默降级</b>成最接近的 16 色之一（{@code #ff8800} → {@code §6}），
     * 颜色精度就这么丢了。开启后输出 {@code §x§f§f§8§8§0§0} 这种逐字符写法。
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** 传统颜色码的字符集：{@code 0-9a-f} 颜色 + {@code k-o} 装饰 + {@code r} 重置。 */
    private static final String CODES = "0123456789abcdefklmnor";

    /** 与 {@link #CODES} 一一对应的 MiniMessage 标签名。 */
    private static final String[] TAGS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white",
            "obfuscated", "bold", "strikethrough", "underlined", "italic", "reset"
    };

    private TextRenderer() {
    }

    /**
     * 渲染为服务端可直接发送的 {@code §} 色码字符串。
     * <p>
     * 输入可以是 MiniMessage 标签、{@code &} 码、{@code §} 码的任意混排。
     */
    @NotNull
    public static String render(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return LEGACY.serialize(parse(raw));
    }

    /**
     * 解析为 Adventure {@link Component}，供需要原生组件的场景使用。
     * <p>
     * 统一关闭斜体：原版会把物品名、实体名等渲染为斜体，任务文本套用后很难看。
     */
    @NotNull
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        Component component;
        try {
            component = MINI.deserialize(normalize(raw));
        } catch (Exception ignored) {
            // 标签非法：退化为纯文本，绝不把标签原文抛给玩家
            component = Component.text(raw);
        }
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /** 去掉全部格式，仅保留纯文本（日志、变量、物品名匹配等场景）。 */
    @NotNull
    public static String strip(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return PLAIN.serialize(parse(raw));
    }

    /** 文本是否为空（含仅含空白）。 */
    public static boolean isBlank(String raw) {
        return raw == null || raw.isBlank();
    }

    /**
     * 把 {@code &} / {@code §} 颜色码统一翻译成 MiniMessage 标签。
     * <p>
     * 同时支持两种十六进制写法：{@code &#RRGGBB} 与 BungeeCord 的
     * {@code §x§R§R§G§G§B§B}。翻译成 {@code <#RRGGBB>} 后交给 MiniMessage。
     * <p>
     * 非法码（如 {@code §} 后面跟普通文字）原样保留，避免吞掉正文里的字符。
     */
    @NotNull
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length() + 16);
        int i = 0;
        while (i < raw.length()) {
            char current = raw.charAt(i);
            if ((current != '&' && current != '\u00A7') || i + 1 >= raw.length()) {
                builder.append(current);
                i++;
                continue;
            }
            char next = raw.charAt(i + 1);

            // 十六进制：&#RRGGBB
            if (next == '#' && i + 8 <= raw.length()) {
                String hex = raw.substring(i + 2, i + 8);
                if (isHex(hex)) {
                    builder.append("<#").append(hex).append('>');
                    i += 8;
                    continue;
                }
            }

            // 十六进制（BungeeCord 逐字符写法）：§x§R§R§G§G§B§B
            if ((next == 'x' || next == 'X') && i + 14 <= raw.length()) {
                StringBuilder hex = new StringBuilder(6);
                boolean valid = true;
                for (int offset = 0; offset < 6; offset++) {
                    char marker = raw.charAt(i + 2 + offset * 2);
                    char digit = raw.charAt(i + 3 + offset * 2);
                    if ((marker != '&' && marker != '\u00A7') || !isHexChar(digit)) {
                        valid = false;
                        break;
                    }
                    hex.append(digit);
                }
                if (valid) {
                    builder.append("<#").append(hex).append('>');
                    i += 14;
                    continue;
                }
            }

            int index = CODES.indexOf(Character.toLowerCase(next));
            if (index < 0) {
                // 不是合法颜色码：原样保留这个字符，继续看下一个
                builder.append(current);
                i++;
                continue;
            }
            builder.append('<').append(TAGS[index]).append('>');
            i += 2;
        }
        return builder.toString();
    }

    private static boolean isHex(String text) {
        if (text.length() != 6) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!isHexChar(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
