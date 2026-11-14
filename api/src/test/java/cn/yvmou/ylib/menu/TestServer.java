package cn.yvmou.ylib.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * 单测里给 Bukkit 装一个最小的 {@link Server}。
 *
 * <h2>为什么需要它</h2>
 * {@code Material.matchMaterial(...)} 会先看服务端缓存再回退到枚举表；而 {@code ItemStack} 的构造、
 * {@code Bukkit.getLogger()} 也都会摸到 {@code Bukkit.getServer()}。因此在没有服务端的单测里，
 * 一个「除 getLogger 外全部返回默认值」的 {@link Proxy} 就够用了——它让菜单里那些**纯逻辑**
 * （布局解析、符号校验、占位符替换、材质解析、点击优先级）都能被钉住，而不必起一个真服务端。
 * <p>
 * 装不上（老版本 JDK 的模块限制之类）也不让测试失败：{@link #ready()} 会返回 false，
 * 依赖服务端的测试自己跳过。
 */
final class TestServer {

    private static boolean installed;
    private static boolean ready;

    private TestServer() {
    }

    static synchronized void install() {
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
                            return Logger.getLogger("YLibMenuTest");
                        }
                        if ("getName".equals(method.getName())) {
                            return "YLibMenuTest";
                        }
                        if ("toString".equals(method.getName())) {
                            return "TestServer";
                        }
                        return defaultValue(method.getReturnType());
                    });
            Bukkit.setServer(server);
            resetMaterialCache();
            ready = true;
        } catch (Throwable unavailable) {
            ready = false;
        }
    }

    /** 服务端装上了吗（装不上时依赖它的测试自己跳过）。 */
    static boolean ready() {
        return ready;
    }

    /**
     * {@code Material.matchMaterial} 会把结果缓存在一个静态表里；不重置的话，
     * 同一句断言在不同用例之间会因为「上一个用例把它缓存成 null」而互相影响。
     */
    private static void resetMaterialCache() {
        try {
            Field cache = Material.class.getDeclaredField("materialMap");
            cache.setAccessible(true);
            Object value = cache.get(null);
            if (value instanceof java.util.Map) {
                ((java.util.Map<?, ?>) value).clear();
            }
            return;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 老版本叫别的名字：换个名字再试
        }
        try {
            Method legacy = Material.class.getDeclaredMethod("setMaterialCache", java.util.Map.class);
            legacy.setAccessible(true);
            legacy.invoke(null, new java.util.HashMap<String, Material>());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 都失败就算了：缓存串味会让个别断言变得不可靠，但不至于把整套测试带崩
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
}
