package cn.yvmou.ylib.api.config;


import cn.yvmou.ylib.exception.YLibException;

/**
 * 配置异常
 * <p>
 * 当配置处理过程中发生错误时抛出此异常。
 * </p>
 *
 * @author yvmou
 * @since 1.0.0-beta5
 */
public class ConfigurationException extends YLibException {
    
    private static final long serialVersionUID = 1L;
    
    /** 配置类 */
    private final Class<?> configurationClass;
    /** 配置路径 */
    private final String configPath;
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     */
    public ConfigurationException(String message) {
        super(message);
        this.configurationClass = null;
        this.configPath = null;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误消息
     * @param cause 原因
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.configurationClass = null;
        this.configPath = null;
    }
    
    /**
     * 构造函数
     * 
     * @param configurationClass 配置类
     * @param message 错误消息
     */
    public ConfigurationException(Class<?> configurationClass, String message) {
        super(message);
        this.configurationClass = configurationClass;
        this.configPath = null;
    }
    
    /**
     * 构造函数
     * 
     * @param configurationClass 配置类
     * @param message 错误消息
     * @param cause 原因
     */
    public ConfigurationException(Class<?> configurationClass, String message, Throwable cause) {
        super(message, cause);
        this.configurationClass = configurationClass;
        this.configPath = null;
    }
    
    /**
     * 构造函数
     * 
     * @param configurationClass 配置类
     * @param configPath 配置路径
     * @param message 错误消息
     */
    public ConfigurationException(Class<?> configurationClass, String configPath, String message) {
        super(message);
        this.configurationClass = configurationClass;
        this.configPath = configPath;
    }
    
    /**
     * 构造函数
     * 
     * @param configurationClass 配置类
     * @param configPath 配置路径
     * @param message 错误消息
     * @param cause 原因
     */
    public ConfigurationException(Class<?> configurationClass, String configPath, String message, Throwable cause) {
        super(message, cause);
        this.configurationClass = configurationClass;
        this.configPath = configPath;
    }
    
    /**
     * 获取配置类
     * 
     * @return 配置类，如果未指定则返回null
     */
    public Class<?> getConfigurationClass() {
        return configurationClass;
    }
    
    /**
     * 获取配置路径
     * 
     * @return 配置路径，如果未指定则返回null
     */
    public String getConfigPath() {
        return configPath;
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (configurationClass != null) {
            sb.append("[").append(configurationClass.getSimpleName()).append("] ");
        }
        
        if (configPath != null) {
            sb.append("[").append(configPath).append("] ");
        }
        
        sb.append(super.getMessage());
        
        return sb.toString();
    }
}