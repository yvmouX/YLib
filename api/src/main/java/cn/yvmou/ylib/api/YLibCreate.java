package cn.yvmou.ylib.api;

import cn.yvmou.ylib.api.enums.ServerType;
import cn.yvmou.ylib.api.exception.YLibException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

public class YLibCreate {
    private static final Logger logger = Logger.getLogger("YLib");

    // 私有构造函数，防止实例化
    private YLibCreate() {
        throw new UnsupportedOperationException("不能被实例化");
    }

    /**
     * 自动创建适合当前服务器的YLib实例
     *
     * @param plugin 插件实例
     * @return YLib API实例
     * @throws YLibException 如果无法创建实例
     */
    @NotNull
    public static YLib create(@NotNull JavaPlugin plugin) throws YLibException {

        logger.info("开始为插件 " + plugin.getName() + " 创建YLib实例...");

        // 检查包重定位
        checkPackageRelocation();

        // 创建对应的实现
        return createImpl(plugin, ServerType.detectServerType());
    }

    /**
     * 创建具体的实现实例
     *
     * @param plugin 插件实例
     * @return YLib API实例
     * @throws YLibException 如果无法创建实例
     */
    @NotNull
    protected static YLib createImpl(@NotNull JavaPlugin plugin, ServerType serverType) throws YLibException {

        if (serverType == ServerType.UNKNOWN) {
            throw new YLibException("无法检测到支持的服务器类型。");
        }

        String implClass = serverType.getImplementationClass();
        if (implClass == null) {
            throw new YLibException("服务器类型 " + serverType.getDisplayName() + " 没有对应的实现类");
        }

        try {
            // 尝试加载实现类
            Class<?> clazz = Class.forName(implClass);

            // 检查是否实现了YLibAPI接口
            if (!YLib.class.isAssignableFrom(clazz)) {
                throw new YLibException("实现类 " + implClass + " 没有实现YLibAPI接口");
            }

            // 获取构造函数
            Constructor<?> constructor = clazz.getConstructor(JavaPlugin.class);

            // 创建实例
            Object instance = constructor.newInstance(plugin);

            logger.info("成功创建 " + serverType.getDisplayName() + " 类型的YLib实例");
            return (YLib) instance;

        } catch (ClassNotFoundException e) {
            throw new YLibException("找不到实现类: " + implClass +
                    ". 请确保您使用的是正确的YLib JAR文件。", e);
        } catch (NoSuchMethodException e) {
            throw new YLibException("实现类 " + implClass + " 缺少必需的构造函数", e);
        } catch (Exception e) {
            throw new YLibException("创建YLib实例时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 检查包重定位
     *
     * @throws YLibException 如果包没有正确重定位
     */
    protected static void checkPackageRelocation() throws YLibException {
        // 运行时替换逗号以避免编译器重定位改变这个字符串
        String originalPackage = "cn,yvmou,ylib,".replace(",", ".");
        String currentPackage = YLib.class.getPackage().getName();

        if (currentPackage.startsWith(originalPackage)) {
            String errorMessage =
                    "****************************************************************\n" +
                            "YLib包没有正确重定位！这将导致与其他使用YLib的插件发生冲突。\n" +
                            "请联系插件开发者并告知他们这个问题！\n" +
                            "当前包名: " + currentPackage + "\n" +
                            "期望包名: [插件包名].libs.ylib 或类似的重定位包名\n" +
                            "****************************************************************";

            logger.severe(errorMessage);
            throw new YLibException("YLib包重定位检查失败，请联系插件开发者修复此问题");
        }
    }
}
