package cn.yvmou.ylib.common.container;

import cn.yvmou.ylib.api.container.ServiceContainer;
import cn.yvmou.ylib.api.container.ServiceNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 简单服务容器实现
 * <p>
 * 提供基础的依赖注入功能，支持单例、工厂和瞬态服务注册。
 * 线程安全的实现，可以在多线程环境中使用。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class SimpleServiceContainer implements ServiceContainer {
    
    private static final Logger LOGGER = Logger.getLogger("YLib-Container");
    
    // 服务注册信息
    private final Map<Class<?>, ServiceRegistration<?>> registrations = new ConcurrentHashMap<>();
    
    // 单例服务实例缓存
    private final Map<Class<?>, Object> singletonInstances = new ConcurrentHashMap<>();
    
    /**
     * 服务注册信息
     */
    private static class ServiceRegistration<T> {
        final Class<T> serviceClass;
        final ServiceType type;
        final Object data; // 可能是实例、工厂或null
        
        ServiceRegistration(Class<T> serviceClass, ServiceType type, Object data) {
            this.serviceClass = serviceClass;
            this.type = type;
            this.data = data;
        }
    }
    
    /**
     * 服务类型枚举
     */
    private enum ServiceType {
        SINGLETON,  // 单例
        FACTORY,    // 工厂
        TRANSIENT   // 瞬态
    }
    
    @Override
    public <T> void registerSingleton(@NotNull Class<T> serviceClass, @NotNull T instance) {
        validateParameters(serviceClass, instance);
        
        if (registrations.containsKey(serviceClass)) {
            throw new IllegalArgumentException("Service already registered: " + serviceClass.getName());
        }
        
        registrations.put(serviceClass, new ServiceRegistration<>(serviceClass, ServiceType.SINGLETON, instance));
        singletonInstances.put(serviceClass, instance);
        
        LOGGER.fine("Registered singleton service: " + serviceClass.getName());
    }
    
    @Override
    public <T> void registerFactory(@NotNull Class<T> serviceClass, @NotNull ServiceFactory<T> factory) {
        validateParameters(serviceClass, factory);
        
        if (registrations.containsKey(serviceClass)) {
            throw new IllegalArgumentException("Service already registered: " + serviceClass.getName());
        }
        
        registrations.put(serviceClass, new ServiceRegistration<>(serviceClass, ServiceType.FACTORY, factory));
        
        LOGGER.fine("Registered factory service: " + serviceClass.getName());
    }
    
    @Override
    public <T> void registerTransient(@NotNull Class<T> serviceClass) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }
        
        if (registrations.containsKey(serviceClass)) {
            throw new IllegalArgumentException("Service already registered: " + serviceClass.getName());
        }
        
        registrations.put(serviceClass, new ServiceRegistration<>(serviceClass, ServiceType.TRANSIENT, null));
        
        LOGGER.fine("Registered transient service: " + serviceClass.getName());
    }
    
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getService(@NotNull Class<T> serviceClass) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }
        
        ServiceRegistration<?> registration = registrations.get(serviceClass);
        if (registration == null) {
            return null;
        }
        
        try {
            switch (registration.type) {
                case SINGLETON:
                    return (T) singletonInstances.get(serviceClass);
                    
                case FACTORY:
                    ServiceFactory<T> factory = (ServiceFactory<T>) registration.data;
                    return factory.create();
                    
                case TRANSIENT:
                    return createTransientInstance(serviceClass);
                    
                default:
                    throw new IllegalStateException("Unknown service type: " + registration.type);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create service instance: " + serviceClass.getName(), e);
            return null;
        }
    }
    
    @Override
    @NotNull
    public <T> T getRequiredService(@NotNull Class<T> serviceClass) {
        T service = getService(serviceClass);
        if (service == null) {
            throw new ServiceNotFoundException(serviceClass, 
                "Required service not found: " + serviceClass.getName() + 
                ". Make sure the service is registered before use.");
        }
        return service;
    }
    
    @Override
    public boolean hasService(@NotNull Class<?> serviceClass) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }
        return registrations.containsKey(serviceClass);
    }
    
    @Override
    public boolean unregisterService(@NotNull Class<?> serviceClass) {
        if (serviceClass == null) {
            throw new IllegalArgumentException("Service class cannot be null");
        }
        
        ServiceRegistration<?> registration = registrations.remove(serviceClass);
        if (registration == null) {
            return false;
        }
        
        // 如果是单例服务，尝试关闭资源
        if (registration.type == ServiceType.SINGLETON) {
            Object instance = singletonInstances.remove(serviceClass);
            if (instance instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) instance).close();
                    LOGGER.fine("Closed service: " + serviceClass.getName());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to close service: " + serviceClass.getName(), e);
                }
            }
        }
        
        LOGGER.fine("Unregistered service: " + serviceClass.getName());
        return true;
    }
    
    @Override
    @NotNull
    public Set<Class<?>> getRegisteredServices() {
        return new HashSet<>(registrations.keySet());
    }
    
    @Override
    public void clear() {
        LOGGER.info("Clearing all services...");
        
        // 关闭所有单例服务
        for (Map.Entry<Class<?>, Object> entry : singletonInstances.entrySet()) {
            Object instance = entry.getValue();
            if (instance instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) instance).close();
                    LOGGER.fine("Closed service: " + entry.getKey().getName());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to close service: " + entry.getKey().getName(), e);
                }
            }
        }
        
        registrations.clear();
        singletonInstances.clear();
        
        LOGGER.info("All services cleared");
    }
    
    /**
     * 创建瞬态服务实例
     */
    @SuppressWarnings("unchecked")
    private <T> T createTransientInstance(Class<T> serviceClass) throws Exception {
        try {
            return serviceClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Service class must have a default constructor: " + serviceClass.getName(), e);
        }
    }
    
    /**
     * 验证参数
     */
    private void validateParameters(Object... params) {
        for (Object param : params) {
            if (param == null) {
                throw new IllegalArgumentException("Parameter cannot be null");
            }
        }
    }
    
    /**
     * 获取服务统计信息
     */
    public String getStatistics() {
        int singletonCount = 0;
        int factoryCount = 0;
        int transientCount = 0;
        
        for (ServiceRegistration<?> registration : registrations.values()) {
            switch (registration.type) {
                case SINGLETON:
                    singletonCount++;
                    break;
                case FACTORY:
                    factoryCount++;
                    break;
                case TRANSIENT:
                    transientCount++;
                    break;
            }
        }
        
        return String.format("Services: %d total (%d singleton, %d factory, %d transient)", 
            registrations.size(), singletonCount, factoryCount, transientCount);
    }
}