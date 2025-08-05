package cn.yvmou.ylib.api.container;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 服务容器接口 - 提供依赖注入功能
 * <p>
 * 服务容器负责管理服务的生命周期，提供服务注册、获取和销毁功能。
 * 支持单例模式和原型模式，以及服务的自动装配。
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 注册服务
 * container.registerSingleton(MyService.class, new MyService());
 * container.registerFactory(OtherService.class, () -> new OtherService());
 * 
 * // 获取服务
 * MyService service = container.getService(MyService.class);
 * 
 * // 检查服务是否存在
 * if (container.hasService(MyService.class)) {
 *     // 使用服务
 * }
 * }</pre>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public interface ServiceContainer {
    
    /**
     * 注册单例服务
     * <p>
     * 单例服务在整个应用程序生命周期中只有一个实例。
     * </p>
     * 
     * @param <T> 服务类型
     * @param serviceClass 服务类
     * @param instance 服务实例
     * @throws IllegalArgumentException 如果参数为null或服务已注册
     */
    <T> void registerSingleton(@NotNull Class<T> serviceClass, @NotNull T instance);
    
    /**
     * 注册服务工厂
     * <p>
     * 每次请求时都会通过工厂创建新的实例（原型模式）。
     * </p>
     * 
     * @param <T> 服务类型
     * @param serviceClass 服务类
     * @param factory 服务工厂
     * @throws IllegalArgumentException 如果参数为null或服务已注册
     */
    <T> void registerFactory(@NotNull Class<T> serviceClass, @NotNull ServiceFactory<T> factory);
    
    /**
     * 注册瞬态服务
     * <p>
     * 每次请求时都会创建新的实例，使用默认构造函数。
     * </p>
     * 
     * @param <T> 服务类型
     * @param serviceClass 服务类
     * @throws IllegalArgumentException 如果参数为null或服务已注册
     */
    <T> void registerTransient(@NotNull Class<T> serviceClass);
    
    /**
     * 获取服务实例
     * 
     * @param <T> 服务类型
     * @param serviceClass 服务类
     * @return 服务实例，如果未注册则返回null
     */
    @Nullable
    <T> T getService(@NotNull Class<T> serviceClass);
    
    /**
     * 获取必需的服务实例
     * 
     * @param <T> 服务类型
     * @param serviceClass 服务类
     * @return 服务实例
     * @throws ServiceNotFoundException 如果服务未注册
     */
    @NotNull
    <T> T getRequiredService(@NotNull Class<T> serviceClass);
    
    /**
     * 检查服务是否已注册
     * 
     * @param serviceClass 服务类
     * @return 如果服务已注册返回true，否则返回false
     */
    boolean hasService(@NotNull Class<?> serviceClass);
    
    /**
     * 注销服务
     * <p>
     * 如果是单例服务且实现了{@link AutoCloseable}接口，会自动调用close方法。
     * </p>
     * 
     * @param serviceClass 服务类
     * @return 如果服务存在并被成功注销返回true，否则返回false
     */
    boolean unregisterService(@NotNull Class<?> serviceClass);
    
    /**
     * 获取所有已注册的服务类型
     * 
     * @return 已注册的服务类型集合
     */
    @NotNull
    java.util.Set<Class<?>> getRegisteredServices();
    
    /**
     * 清空所有服务
     * <p>
     * 会自动关闭所有实现了{@link AutoCloseable}接口的单例服务。
     * </p>
     */
    void clear();
    
    /**
     * 服务工厂接口
     * 
     * @param <T> 服务类型
     */
    @FunctionalInterface
    interface ServiceFactory<T> {
        /**
         * 创建服务实例
         * 
         * @return 服务实例
         * @throws Exception 如果创建失败
         */
        @NotNull
        T create() throws Exception;
    }
}