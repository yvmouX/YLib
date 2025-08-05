package cn.yvmou.ylib.api.container;

import cn.yvmou.ylib.api.exception.YLibException;

/**
 * 服务未找到异常
 * <p>
 * 当尝试获取未注册的必需服务时抛出此异常。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class ServiceNotFoundException extends YLibException {
    
    private static final long serialVersionUID = 1L;
    
    /** 服务类 */
    private final Class<?> serviceClass;
    
    /**
     * 构造函数
     * 
     * @param serviceClass 未找到的服务类
     */
    public ServiceNotFoundException(Class<?> serviceClass) {
        super("Service not found: " + serviceClass.getName());
        this.serviceClass = serviceClass;
    }
    
    /**
     * 构造函数
     * 
     * @param serviceClass 未找到的服务类
     * @param message 错误消息
     */
    public ServiceNotFoundException(Class<?> serviceClass, String message) {
        super(message);
        this.serviceClass = serviceClass;
    }
    
    /**
     * 构造函数
     * 
     * @param serviceClass 未找到的服务类
     * @param message 错误消息
     * @param cause 原因
     */
    public ServiceNotFoundException(Class<?> serviceClass, String message, Throwable cause) {
        super(message, cause);
        this.serviceClass = serviceClass;
    }
    
    /**
     * 获取未找到的服务类
     * 
     * @return 服务类
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }
}