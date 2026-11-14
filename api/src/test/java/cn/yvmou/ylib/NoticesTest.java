package cn.yvmou.ylib;

import cn.yvmou.ylib.utils.LegacyCodes;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 给命令发送者的提示：**只发聊天框，不碰日志**。
 *
 * <h2>这个类要钉住的核心就是「没有副作用」</h2>
 * 它从 {@code Logger} 里拆出来，原因之一就是旧路径有副作用：{@code LoggerImpl} 会把参数里任何
 * {@code Throwable} 以 {@code SEVERE} 写进控制台、还附一句 {@code "YLib error"}。
 * 于是「告诉玩家没权限」这种再普通不过的提示，会在控制台留下一行莫名其妙的错误日志。
 * {@link #doesNotTouchConsole()} 就是钉这条的。
 *
 * <h2>为什么用动态代理造 sender</h2>
 * {@code CommandSender} 的方法跨版本会增减（{@code sendMessage(UUID, String...)} 之类），
 * 手写实现会在换一个 API 版本时编译不过。这里只关心 {@code sendMessage(String)}，
 * 其余一律返回默认值。
 */
class NoticesTest {

    /** 收集控制台日志，用来断言「提示没有写日志」。 */
    private static final class ConsoleSpy extends Handler {
        final List<LogRecord> records = new ArrayList<LogRecord>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static ConsoleSpy console;

    @BeforeAll
    static void setUp() {
        installFakeServer();
        console = new ConsoleSpy();
        Logger.getLogger("").addHandler(console);
    }

    @AfterEach
    void clear() {
        console.records.clear();
    }

    /** 只记消息的假 sender；其余方法一律返回默认值。 */
    private static CommandSender sender(final List<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(
                NoticesTest.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if ("sendMessage".equals(method.getName()) && args != null && args.length > 0
                            && args[args.length - 1] instanceof String) {
                        messages.add((String) args[args.length - 1]);
                        return null;
                    }
                    if ("getName".equals(method.getName())) {
                        return "FakeSender";
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeSender";
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static void installFakeServer() {
        try {
            Server server = (Server) Proxy.newProxyInstance(
                    NoticesTest.class.getClassLoader(),
                    new Class<?>[]{Server.class},
                    (proxy, method, args) -> {
                        if ("getLogger".equals(method.getName())) {
                            return Logger.getLogger("");
                        }
                        if ("getName".equals(method.getName())) {
                            return "YLibNoticesTest";
                        }
                        if ("toString".equals(method.getName())) {
                            return "TestServer";
                        }
                        return defaultValue(method.getReturnType());
                    });
            Bukkit.setServer(server);
        } catch (Throwable unavailable) {
            // 装不上就算了：Notices 本身不碰 Server，只有用来断言「没写日志」的 handler 需要它
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == void.class) {
            return null;
        }
        if (type == char.class) {
            return Character.valueOf('\0');
        }
        if (type == byte.class) {
            return Byte.valueOf((byte) 0);
        }
        if (type == short.class) {
            return Short.valueOf((short) 0);
        }
        if (type == int.class) {
            return Integer.valueOf(0);
        }
        if (type == long.class) {
            return Long.valueOf(0L);
        }
        if (type == float.class) {
            return Float.valueOf(0F);
        }
        return Double.valueOf(0D);
    }

    @Test
    @DisplayName("info/warn/error 都只发给发送者，正文经过色码转换")
    void sendsToSenderOnly() {
        List<String> messages = new ArrayList<String>();
        CommandSender sender = sender(messages);
        Notices.info(sender, "&a普通提示");
        Notices.warn(sender, "&e小心");
        Notices.error(sender, "&c坏了");

        assertEquals(3, messages.size(), messages.toString());
        assertTrue(messages.get(0).endsWith(LegacyCodes.toSection("&a普通提示")), messages.get(0));
        assertTrue(messages.get(1).endsWith(LegacyCodes.toSection("&e小心")), messages.get(1));
        assertTrue(messages.get(2).endsWith(LegacyCodes.toSection("&c坏了")), messages.get(2));
    }

    @Test
    @DisplayName("info 不带前缀：玩家刚敲完命令，回执里再重复插件名只是噪音")
    void infoHasNoPrefix() {
        List<String> messages = new ArrayList<String>();
        Notices.info(sender(messages), "已保存");
        assertEquals(LegacyCodes.toSection("已保存"), messages.get(0));
    }

    @Test
    @DisplayName("warn / error 用颜色区分，不用「WARN」字样（那是日志级别，不是给玩家看的）")
    void warnAndErrorUseColor() {
        List<String> messages = new ArrayList<String>();
        CommandSender sender = sender(messages);
        Notices.warn(sender, "小心");
        Notices.error(sender, "坏了");

        assertTrue(messages.get(0).startsWith(LegacyCodes.section() + "e"), messages.get(0));
        assertTrue(messages.get(1).startsWith(LegacyCodes.section() + "c"), messages.get(1));
        assertFalse(messages.get(0).contains("WARN"));
        assertFalse(messages.get(1).contains("ERROR"));
    }

    @Test
    @DisplayName("不碰控制台：玩家提示不该有日志副作用（这正是它从 Logger 拆出来的原因）")
    void doesNotTouchConsole() {
        List<String> messages = new ArrayList<String>();
        CommandSender sender = sender(messages);
        Notices.info(sender, "普通提示");
        Notices.warn(sender, "警告提示");
        Notices.error(sender, "错误提示");

        assertEquals(3, messages.size());
        assertTrue(console.records.isEmpty(),
                "给玩家发提示不该写控制台，但实际写了 " + console.records.size() + " 条");
    }
}
