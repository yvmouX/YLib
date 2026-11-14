package cn.yvmou.ylib.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 传统色码的转换与剥离：{@code &}/{@code §} ↔ 段落符，以及「只留正文」。
 *
 * <h2>为什么下沉到 utils</h2>
 * 这套转换原本有三份实现，功能各不相同（一份没有十六进制、一份输出 MiniMessage 标签、
 * 一份多支持 {@code &&} 转义），散在 logger / text / menu 三处。落在 util 层之后，
 * <b>需要用它的地方各取所需，而不必彼此依赖</b>——logger 只想要「转成字符串」，
 * menu 只想要「转成字符串」，text 要的是另一件事（MiniMessage 标签）因此仍走自己那条路。
 *
 * <h2>支持哪些写法</h2>
 * <ul>
 *   <li>{@code &0-f} / {@code &k-o} / {@code &r}：颜色与装饰码；</li>
 *   <li>{@code &#RRGGBB}：十六进制；</li>
 *   <li>{@code §x§R§R§G§G§B§B}：BungeeCord 逐字符十六进制，原样识别并放行；</li>
 *   <li>{@code &&}：一个<b>字面量</b> {@code &}（写「&amp;」本身时用）。</li>
 * </ul>
 * 非法码（{@code &} 后面跟普通文字）<b>原样保留</b>，避免吞掉正文里的字符。
 */
public final class LegacyCodes {

    /** 与 {@code 0-9a-f} 颜色 + {@code k-o} 装饰 + {@code r} 重置一一对应。 */
    private static final String CODES = "0123456789abcdefklmnor";

    /** 段落符：Minecraft 实际认的那个字符。 */
    private static final char SECTION = '\u00A7';

    private LegacyCodes() {
    }

    /** 单个合法色码字符（{@code 0-9a-f} 或 {@code k-o} 或 {@code r}）。 */
    public static boolean isCode(char c) {
        return CODES.indexOf(Character.toLowerCase(c)) >= 0;
    }

    /** 段落符。需要自己拼色码串时用它，避免各处硬编码 {@code '\u00A7'}。 */
    public static char section() {
        return SECTION;
    }

    /**
     * {@code &} 色码 → {@code §} 色码；{@code null} / 空串原样返回。
     * <p>
     * 已经是 {@code §} 的写法会被识别并保留（不会被当作普通字符），因此对同一个字符串重复调用是幂等的。
     */
    @Nullable
    public static String toSection(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length() + 16);
        int index = 0;
        while (index < raw.length()) {
            char current = raw.charAt(index);
            if ((current != '&' && current != SECTION) || index + 1 >= raw.length()) {
                out.append(current);
                index++;
                continue;
            }
            char next = raw.charAt(index + 1);

            // 转义：&& → 一个字面量 &
            if (next == '&') {
                out.append('&');
                index += 2;
                continue;
            }

            // 十六进制：&#RRGGBB
            if (next == '#' && index + 8 <= raw.length() && isHex(raw.substring(index + 2, index + 8))) {
                appendHex(out, raw.substring(index + 2, index + 8));
                index += 8;
                continue;
            }

            // 十六进制（已经是 §x§R§R… 的写法）：原样放行，别把 § 后的 x 当普通字符
            if (next == 'x' || next == 'X') {
                String hex = hexSequenceAt(raw, index);
                if (hex != null) {
                    appendHex(out, hex);
                    index += 14;
                    continue;
                }
            }

            if (!isCode(next)) {
                // 不是合法色码：原样保留这个字符，继续看下一个
                out.append(current);
                index++;
                continue;
            }
            // 色码统一输出小写：&A 与 &a 是同一个颜色，输出 §A 虽然多数客户端也认，
            // 但会让「同一份文案经过不同路径得到的字节不同」——比对与幂等断言都会因此失败
            out.append(SECTION).append(Character.toLowerCase(next));
            index += 2;
        }
        return out.toString();
    }

    /**
     * 去掉全部色码，只留正文（日志、比对、匹配用）。
     * <p>
     * {@code &&} 会被还原成一个 {@code &}，而不是被当作色码删掉。
     */
    @Nullable
    public static String strip(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        int index = 0;
        while (index < raw.length()) {
            char current = raw.charAt(index);
            if (current != '&' && current != SECTION) {
                out.append(current);
                index++;
                continue;
            }
            if (index + 1 >= raw.length()) {
                // 结尾孤零零一个 '&'：它是正文的一部分，保留。
                // （丢掉它会让 strip 与 toSection 对同一串给出不同结果——后者是保留的）
                out.append(current);
                index++;
                continue;
            }
            char next = raw.charAt(index + 1);
            if (next == '&') {
                out.append('&');
                index += 2;
                continue;
            }
            if (next == '#') {
                int consumed = lengthOfHex(raw, index);
                if (consumed > 0) {
                    index += consumed;
                    continue;
                }
            }
            if (next == 'x' || next == 'X') {
                if (hexSequenceAt(raw, index) != null) {
                    index += 14;
                    continue;
                }
            }
            if (isCode(next)) {
                index += 2;
                continue;
            }
            // 非法码：两个字符都要留下。只 append '&' 会把后面的字符吞掉（"A&B" → "A"）
            out.append(current).append(next);
            index += 2;
        }
        return out.toString();
    }

    // ---------- 内部 ----------

    /** 把 6 位十六进制写成 {@code §x§R§R§G§G§B§B}。 */
    private static void appendHex(@NotNull StringBuilder out, @NotNull String hex) {
        out.append(SECTION).append('x');
        for (int digit = 0; digit < 6; digit++) {
            out.append(SECTION).append(hex.charAt(digit));
        }
    }

    /** {@code &#RRGGBB} 的长度；不是这个写法返回 0。 */
    private static int lengthOfHex(@NotNull String raw, int at) {
        if (at + 8 > raw.length()) {
            return 0;
        }
        return isHex(raw.substring(at + 2, at + 8)) ? 8 : 0;
    }

    /**
     * 从 {@code at} 开始读一段 {@code §x§R§R§G§G§B§B}，返回那 6 位十六进制；
     * 不是这个写法返回 {@code null}。分隔符允许是 {@code &} 或 {@code §}。
     */
    @Nullable
    private static String hexSequenceAt(@NotNull String raw, int at) {
        if (at + 14 > raw.length()) {
            return null;
        }
        StringBuilder hex = new StringBuilder(6);
        for (int offset = 0; offset < 6; offset++) {
            char marker = raw.charAt(at + 2 + offset * 2);
            char digit = raw.charAt(at + 3 + offset * 2);
            if ((marker != '&' && marker != SECTION) || !isHexChar(digit)) {
                return null;
            }
            hex.append(digit);
        }
        return hex.toString();
    }

    private static boolean isHex(@NotNull String text) {
        if (text.length() != 6) {
            return false;
        }
        for (int index = 0; index < 6; index++) {
            if (!isHexChar(text.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
