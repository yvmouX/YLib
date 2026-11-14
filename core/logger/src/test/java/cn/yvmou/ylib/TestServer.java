package cn.yvmou.ylib;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * 单测里给 Bukkit 装一个最小的 {@link Server}。
 * <p>
 * 这里只需要 {@code Bukkit.getLogger()} 能用（日志实现直接写它），因此代理比
 * {@code api} 模块里那个同名助手简单得多——不用管材质缓存之类的东西。
 * <p>
 * 装不上（老 JDK 的模块限制之类）不让测试失败：{@link #ready()} 返回 false，
 * 依赖它的用例自己决定要不要跳过。
 */
public final class TestServer {

    private static boolean installed;
    private static boolean ready;

    private TestServer() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        try {
            Server server = (Server) Proxy.newProxyInstance(
                    TestServer.class.getClassLoader(),
                    new Class<?>[]{Server.class},
                    (proxy, method, args) -> {
                        if ("getLogger".equals(method.getName())) {
                            // 必须是**根 logger**：用例会往它上面挂 handler 来收集记录
                            return Logger.getLogger("");
                        }
                        if ("getName".equals(method.getName())) {
                            return "YLibLoggerTest";
                        }
                        if ("toString".equals(method.getName())) {
                            return "TestServer";
                        }
                        return defaultValue(method.getReturnType());
                    });
            Bukkit.setServer(server);
            ready = true;
        } catch (Throwable unavailable) {
            ready = false;
        }
    }

    /** 服务端装上了吗。 */
    public static boolean ready() {
        return ready;
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
}
