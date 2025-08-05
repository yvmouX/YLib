package cn.yvmou.ylib.api;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

/**
 * YLib工厂类，用于自动创建适合当前服务器类型的YLib实例
 * 
 * @author yvmou
 * @since 1.0.0-beta5
 */
public final class YLibFactory {
    
    private static final Logger LOGGER = Logger.getLogger("YLib-Factory");
    
    // 私有构造函数，防止实例化
    private YLibFactory() {
        throw new UnsupportedOperationException("YLibFactory不能被实例化");
    }
    
    /**
     * 自动创建适合当前服务器的YLib实例
     * 
     * @param plugin 插件实例
     * @return YLib API实例
     * @throws YLibException 如果无法创建实例
     */
    @NotNull
    public static YLibAPI create(@NotNull JavaPlugin plugin) throws YLibException {
        if (plugin == null) {
            throw new IllegalArgumentException("插件实例不能为null");
        }
        
        LOGGER.info("开始为插件 " + plugin.getName() + " 创建YLib实例...");
        
        // 检查包重定位
        checkPackageRelocation();
        
        // 自动检测服务器类型
        ServerType serverType = ServerType.detectServerType();
        
        // 创建对应的实现
        return createImplementation(plugin, serverType);
    }
    
    /**
     * 手动指定服务器类型创建YLib实例
     * 
     * @param plugin 插件实例
     * @param serverType 服务器类型
     * @return YLib API实例
     * @throws YLibException 如果无法创建实例
     */
    @NotNull
    public static YLibAPI create(@NotNull JavaPlugin plugin, @NotNull ServerType serverType) throws YLibException {
        if (plugin == null) {
            throw new IllegalArgumentException("插件实例不能为null");
        }
        if (serverType == null) {
            throw new IllegalArgumentException("服务器类型不能为null");
        }
        
        LOGGER.info("为插件 " + plugin.getName() + " 手动创建 " + serverType.getDisplayName() + " 类型的YLib实例...");
        
        // 检查包重定位
        checkPackageRelocation();
        
        return createImplementation(plugin, serverType);
    }
    
    /**
     * 创建具体的实现实例
     * 
     * @param plugin 插件实例
     * @param serverType 服务器类型
     * @return YLib API实例
     * @throws YLibException 如果无法创建实例
     */
    @NotNull
    private static YLibAPI createImplementation(@NotNull JavaPlugin plugin, @NotNull ServerType serverType) throws YLibException {
        if (serverType == ServerType.UNKNOWN) {
            throw new YLibException("无法检测到支持的服务器类型。请确保您使用的是Folia、Paper或Spigot服务器。");
        }
        
        String implementationClass = serverType.getImplementationClass();
        if (implementationClass == null) {
            throw new YLibException("服务器类型 " + serverType.getDisplayName() + " 没有对应的实现类");
        }
        
        try {
            // 尝试加载实现类
            Class<?> clazz = Class.forName(implementationClass);
            
            // 检查是否实现了YLibAPI接口
            if (!YLibAPI.class.isAssignableFrom(clazz)) {
                throw new YLibException("实现类 " + implementationClass + " 没有实现YLibAPI接口");
            }
            
            // 获取构造函数
            Constructor<?> constructor = clazz.getConstructor(JavaPlugin.class);
            
            // 创建实例
            Object instance = constructor.newInstance(plugin);
            
            LOGGER.info("成功创建 " + serverType.getDisplayName() + " 类型的YLib实例");
            return (YLibAPI) instance;
            
        } catch (ClassNotFoundException e) {
            throw new YLibException("找不到实现类: " + implementationClass + 
                ". 请确保您使用的是正确的YLib JAR文件。", e);
        } catch (NoSuchMethodException e) {
            throw new YLibException("实现类 " + implementationClass + " 缺少必需的构造函数", e);
        } catch (Exception e) {
            throw new YLibException("创建YLib实例时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查包重定位
     * 
     * @throws YLibException 如果包没有正确重定位
     */
    private static void checkPackageRelocation() throws YLibException {
        // 运行时替换逗号以避免编译器重定位改变这个字符串
        String originalPackage = "cn,yvmou,ylib,".replace(",", ".");
        String currentPackage = YLibFactory.class.getPackage().getName();
        
        if (currentPackage.startsWith(originalPackage)) {
            String errorMessage = 
                "****************************************************************\n" +
                "YLib包没有正确重定位！这将导致与其他使用YLib的插件发生冲突。\n" +
                "请联系插件开发者并告知他们这个问题！\n" +
                "当前包名: " + currentPackage + "\n" +
                "期望包名: [插件包名].libs.ylib 或类似的重定位包名\n" +
                "****************************************************************";
            
            LOGGER.severe(errorMessage);
            throw new YLibException("YLib包重定位检查失败，请联系插件开发者修复此问题");
        }
        
        LOGGER.info("包重定位检查通过");
    }
    
    /**
     * 获取当前检测到的服务器类型
     * 
     * @return 服务器类型
     */
    @NotNull
    public static ServerType getDetectedServerType() {
        return ServerType.detectServerType();
    }
    
    /**
     * 检查是否支持指定的服务器类型
     * 
     * @param serverType 服务器类型
     * @return 如果支持返回true，否则返回false
     */
    public static boolean isSupported(@NotNull ServerType serverType) {
        return serverType != ServerType.UNKNOWN && 
               serverType.getImplementationClass() != null &&
               serverType.selfCheck();
    }
    
    /**
     * 获取YLib版本信息
     * 
     * @return 版本信息字符串
     */
    @NotNull
    public static String getVersion() {
        return "1.0.0-beta5";
    }
    
    /**
     * 获取支持的服务器类型列表
     * 
     * @return 支持的服务器类型数组
     */
    @NotNull
    public static ServerType[] getSupportedServerTypes() {
        return new ServerType[]{ServerType.FOLIA, ServerType.PAPER, ServerType.SPIGOT};
    }
}