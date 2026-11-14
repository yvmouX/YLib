package cn.yvmou.ylib.message;

import org.jetbrains.annotations.NotNull;

/**
 * 语言文件的数字占位符替换（{@code {0}}、{@code {1}}…）。
 * <p>
 * 参数按<b>字面</b>插入：只做纯文本拼接，不解析 MiniMessage 标签、不展开正则替换里的
 * {@code $}/{@code \} 引用、也不改动参数里的 {@code §} 色码。
 * </p>
 * <p>
 * 没提供参数的占位符会被静默移除（避免 {@code {2}} 泄漏到聊天栏）；不合法的花括号
 * （如 {@code {abc}}）原样保留。
 * </p>
 */
final class MessageFormatter {

    private MessageFormatter() {
    }

    @NotNull
    static String format(@NotNull String message, @NotNull Object... args) {
        if (args.length == 0) {
            return message;
        }
        // 第一遍：只在扫描到合法占位符 {数字} 的位置打标记。
        // 之所以要标记而不是「替换 + 清理」两步走，是因为后者的清理必须靠正则改写整条消息，
        // 而正则替换的 replacement 会展开 $ 引用——参数里带 "$1" 时直接抛
        // IllegalArgumentException: Illegal group reference。
        // 数字超长（如 {99999999999999}）按越界处理，不会被解析成 int 而抛异常。
        boolean[] placeholder = new boolean[message.length() + 1];
        int i = 0;
        while (i < message.length()) {
            if (message.charAt(i) == '{') {
                int end = i + 1;
                while (end < message.length() && isDigit(message.charAt(end))) {
                    end++;
                }
                // 至少要有一位数字，且必须以 } 收尾，才算是占位符
                if (end > i + 1 && end < message.length() && message.charAt(end) == '}') {
                    placeholder[i] = true;
                    i = end + 1;
                    continue;
                }
            }
            i++;
        }

        // 第二遍：按标记拼接。参数只经过 append 落进结果，因此 100% 字面——
        // 既不解析标签，也不会被当成正则替换的 replacement。
        StringBuilder result = new StringBuilder(message.length() + 16);
        int index = 0;
        while (index < message.length()) {
            if (!placeholder[index]) {
                result.append(message.charAt(index++));
                continue;
            }
            int close = message.indexOf('}', index);
            int argIndex = parseIndex(message, index + 1, close);
            // argIndex < 0 表示数字超出 int 范围，同样按越界（移除）处理
            if (argIndex >= 0 && argIndex < args.length) {
                result.append(String.valueOf(args[argIndex]));
            }
            // 未提供的占位符：什么都不追加，等价于移除
            index = close + 1;
        }
        return result.toString();
    }

    /** 解析占位符里的下标；超出 int 范围返回 -1。 */
    private static int parseIndex(@NotNull String message, int from, int to) {
        if (to - from > 9) {
            return -1;
        }
        int value = 0;
        for (int i = from; i < to; i++) {
            value = value * 10 + (message.charAt(i) - '0');
        }
        return value;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
