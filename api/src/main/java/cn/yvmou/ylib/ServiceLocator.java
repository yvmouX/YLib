package cn.yvmou.ylib;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Service implementation lookup with a relocation-proof fallback.
 * <p>
 * First tries the standard {@link ServiceLoader} mechanism (extension point for
 * third parties). If nothing is found, the implementation class is resolved by
 * name inside the service interface's own package - since shading relocates
 * both the interface and the implementation together, the fallback keeps
 * working after relocation, unlike META-INF/services files which rely on the
 * class loader exposing jar resources (not guaranteed on Paper/Folia plugin
 * class loaders).
 */
public final class ServiceLocator {

    private ServiceLocator() {
    }

    /**
     * 查找服务实现。
     * <p>
     * 优先使用 {@link ServiceLoader}；找不到时回退为"接口所在包 + 实现类简单名"的反射定位。
     * 回退使用 {@code serviceType.getClassLoader()} 而不是线程上下文类加载器，
     * 因为加载接口的类加载器始终是插件自己的类加载器，与调用线程无关。
     *
     * @param serviceType     服务接口类型
     * @param implSimpleName  实现类简单名（与接口同包）
     * @param <T>             服务类型
     * @return 服务实现实例
     * @throws YLibException 两种方式都找不到时抛出，原始原因作为 cause 保留
     */
    public static <T> T locate(Class<T> serviceType, String implSimpleName) {
        Throwable failure = null;

        // 1) Standard ServiceLoader lookup
        try {
            ServiceLoader<T> loader = ServiceLoader.load(serviceType);
            Iterator<T> iterator = loader.iterator();
            while (iterator.hasNext()) {
                try {
                    T provider = iterator.next();
                    if (provider != null) return provider;
                } catch (ServiceConfigurationError error) {
                    if (failure == null) failure = error;
                }
            }
        } catch (Throwable throwable) {
            if (failure == null) failure = throwable;
        }

        // 2) Relocation-proof fallback: same-package class lookup
        Package servicePackage = serviceType.getPackage();
        if (servicePackage != null) {
            try {
                Class<?> impl = Class.forName(servicePackage.getName() + "." + implSimpleName, true, serviceType.getClassLoader());
                return serviceType.cast(impl.getDeclaredConstructor().newInstance());
            } catch (Throwable throwable) {
                if (failure == null) failure = throwable;
            }
        }

        throw new YLibException("Service implementation '" + implSimpleName + "' not found for "
                + serviceType.getName() + ". Ensure the corresponding module is included.", failure);
    }
}
