package cn.yvmou.ylib.api;

import cn.yvmou.ylib.enums.ServerType;
import cn.yvmou.ylib.exception.YLibException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class YLibBuilder {
    private static final Logger logger = Logger.getLogger("YLib");

    private YLibBuilder() {
        throw new UnsupportedOperationException("You can't instantiate this class.");
    }

    public static YLib create(@NotNull JavaPlugin plugin) throws YLibException {
        checkPackageRelocation();
        return createImpl(plugin, ServerType.detectServerType());
    }


    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */
    @NotNull
    private static YLib createImpl(@NotNull JavaPlugin plugin, ServerType serverType) throws YLibException {
        if (serverType == ServerType.UNKNOWN) {
            throw new YLibException("YLib cannot detect your server type.");
        }

        String implClass = "cn.yvmou.ylib.YLibImpl";
        try {
            Class<?> clazz = Class.forName(implClass);

            Constructor<?> constructor = clazz.getDeclaredConstructor(JavaPlugin.class);
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(plugin);

            return (YLib) instance;
        } catch (Exception e) {
            throw new YLibException("Unknow Error", e);
        }
    }


    private static void checkPackageRelocation() throws YLibException {
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