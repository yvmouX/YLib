package cn.yvmou.ylib.logger;

import org.jetbrains.annotations.NotNull;

/**
 * 日志文本的格式化：{@code {}} 占位符 + 传统色码。
 *
 * <p>
 * {@code {}} 的语义与 SLF4J 一致：按出现顺序逐个替换；参数多于占位符时**忽略多余的**
 * （这比抛异常好——多传一个参数不该让一条排查用的日志变成新的故障）。
 *
 * <p>
 * 颜色转换走 {@link cn.yvmou.ylib.utils.LegacyCodes}（与菜单共用同一份实现）。
 * <b>注意</b>：转换结果带 {@code §} 色码，写进控制台日志前应当用
 * {@link cn.yvmou.ylib.utils.LegacyCodes#strip} 剥掉——日志系统不翻译颜色码。
 */
public final class LoggerUtil {

    private LoggerUtil() {
    }

    /**
     * 按 {@code {}} 占位符格式化，并把 {@code &} 色码转成 {@code §}。
     */
    public static String formatMessage(@NotNull String format, @NotNull Object... args) {
        if (args == null || args.length == 0) {
            return cn.yvmou.ylib.utils.LegacyCodes.toSection(format);
        }

        // 手写逐段替换而不是正则：日志是热路径，正则的编译与回溯都不划算
        StringBuilder text = new StringBuilder(format.length() + 50);
        int argIndex = 0;
        int lastIndex = 0;

        while (argIndex < args.length) {
            int placeholder = format.indexOf("{}", lastIndex);
            if (placeholder < 0) {
                break;
            }
            text.append(format, lastIndex, placeholder);
            text.append(args[argIndex++]);
            lastIndex = placeholder + 2;
        }
        text.append(format.substring(lastIndex));

        return cn.yvmou.ylib.utils.LegacyCodes.toSection(text.toString());
    }
}
