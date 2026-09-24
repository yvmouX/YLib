package cn.yvmou.ylib.menu.internal;

/**
 * 传统 {@code &} 色码 → {@code §} 色码。
 *
 * <h2>为什么本包自己转一遍，而不是复用 {@code TextRenderer}</h2>
 * {@code TextRenderer} 把 {@code &} 码翻译成 <b>MiniMessage 标签</b>再渲染，也就是把整串当模板解析。
 * 菜单里行不通：物品名与 lore 是「模板 + 运行时值」拼出来的——玩家昵称、任务名、服务器自定义文本里
 * 完全可能出现 {@code <} 或 {@code <red>} 这类字符，那些字符会被当成 MiniMessage 标签**吃掉**或者让整串
 * 解析失败退化成纯文本。菜单文本因此走最笨也最安全的一条路：只认 {@code &}/{@code §} 色码，不做标签解析。
 * <p>
 * 支持 {@code &#RRGGBB} 与 BungeeCord 的 {@code §x§R§R§G§G§B§B} 两种十六进制写法；非法码
 * （{@code &} 后面跟普通文字）原样保留，避免吞掉正文里的字符。
 * <p>
 * 转义：{@code &&} 输出一个字面量 {@code &}（写「&」本身时用），这是唯一一个本包额外支持的写法。
 */
public final class LegacyText {

    /** 与 {@code 0-9a-f} 颜色 + {@code k-o} 装饰 + {@code r} 重置一一对应。 */
    private static final String CODES = "0123456789abcdefklmnor";

    /** 输出用的色码字符。 */
    private static final char SECTION = '\u00A7';

    private LegacyText() {
    }

    /** 把 {@code &} 色码翻译成 {@code §} 色码；{@code null} / 空串原样返回。 */
    public static String render(String raw) {
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
            if (next == '#' && index + 8 <= raw.length()) {
                String hex = raw.substring(index + 2, index + 8);
                if (isHex(hex)) {
                    out.append(SECTION).append('x');
                    for (int digit = 0; digit < 6; digit++) {
                        out.append(SECTION).append(hex.charAt(digit));
                    }
                    index += 8;
                    continue;
                }
            }

            // 十六进制（已经是 §x§R§R… 的写法）：原样放行，别把 § 后的 x 当普通字符
            if ((next == 'x' || next == 'X') && index + 14 <= raw.length()) {
                StringBuilder hex = new StringBuilder(6);
                boolean valid = true;
                for (int offset = 0; offset < 6; offset++) {
                    char marker = raw.charAt(index + 2 + offset * 2);
                    char digit = raw.charAt(index + 3 + offset * 2);
                    if ((marker != '&' && marker != SECTION) || !isHexChar(digit)) {
                        valid = false;
                        break;
                    }
                    hex.append(digit);
                }
                if (valid) {
                    out.append(SECTION).append('x');
                    for (int digit = 0; digit < 6; digit++) {
                        out.append(SECTION).append(hex.charAt(digit));
                    }
                    index += 14;
                    continue;
                }
            }

            int code = CODES.indexOf(Character.toLowerCase(next));
            if (code < 0) {
                // 不是合法色码：原样保留这个字符
                out.append(current);
                index++;
                continue;
            }
            out.append(SECTION).append(next);
            index += 2;
        }
        return out.toString();
    }

    /** 去掉 {@code &}/{@code §} 色码，只留正文（日志、比对用）。 */
    public static String strip(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        int index = 0;
        while (index < raw.length()) {
            char current = raw.charAt(index);
            if (current == '&' || current == SECTION) {
                if (index + 1 >= raw.length()) {
                    index++;
                    continue;
                }
                char next = raw.charAt(index + 1);
                if (next == '&') {
                    out.append('&');
                    index += 2;
                    continue;
                }
                // 十六进制：跳过整段色码
                if (next == '#') {
                    int consumed = lengthOfHex(raw, index);
                    if (consumed > 0) {
                        index += consumed;
                        continue;
                    }
                }
                if (next == 'x' || next == 'X') {
                    int consumed = lengthOfHexSequence(raw, index);
                    if (consumed > 0) {
                        index += consumed;
                        continue;
                    }
                }
                if (CODES.indexOf(Character.toLowerCase(next)) >= 0) {
                    index += 2;
                    continue;
                }
                out.append(current);
                index++;
                continue;
            }
            out.append(current);
            index++;
        }
        return out.toString();
    }

    /** {@code &#RRGGBB} 的长度；不是这个写法返回 0。 */
    private static int lengthOfHex(String raw, int at) {
        if (at + 8 > raw.length()) {
            return 0;
        }
        return isHex(raw.substring(at + 2, at + 8)) ? 8 : 0;
    }

    /** {@code &x&R&R&G&G&B&B} 的长度；不是这个写法返回 0。 */
    private static int lengthOfHexSequence(String raw, int at) {
        if (at + 14 > raw.length()) {
            return 0;
        }
        for (int offset = 0; offset < 6; offset++) {
            char marker = raw.charAt(at + 2 + offset * 2);
            char digit = raw.charAt(at + 3 + offset * 2);
            if ((marker != '&' && marker != SECTION) || !isHexChar(digit)) {
                return 0;
            }
        }
        return 14;
    }

    private static boolean isHex(String text) {
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
