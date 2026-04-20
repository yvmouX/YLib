package cn.yvmou.ylib.logger;

import org.jetbrains.annotations.NotNull;

public class LoggerUtil {
    /**
     * 格式化消息，支持 {} 占位符
     */
    protected static String formatMessage(@NotNull String format, @NotNull Object... args) {
        if (args == null || args.length == 0) {
            return translateColorCodes(format);
        }

        // 简单的 {} 替换实现，比正则更高效
        StringBuilder sb = new StringBuilder(format.length() + 50);
        int argIndex = 0;
        int lastIndex = 0;

        while (argIndex < args.length) {
            int placeholderIndex = format.indexOf("{}", lastIndex);
            if (placeholderIndex == -1) {
                break;
            }

            sb.append(format, lastIndex, placeholderIndex);
            sb.append(args[argIndex++]);
            lastIndex = placeholderIndex + 2;
        }

        sb.append(format.substring(lastIndex));

        return translateColorCodes(sb.toString());
    }

    /**
     * 将 & 转为 §（仅限合法MC颜色代码）
     */
    private static String translateColorCodes(String text) {
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && isColorCode(chars[i + 1])) {
                chars[i] = '§';
            }
        }

        return new String(chars);
    }

    private static boolean isColorCode(char c) {
        return "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(c) != -1;
    }
}
