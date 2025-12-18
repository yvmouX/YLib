package cn.yvmou.ylib.impl.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * 版本工具类 - 提供服务器版本检测功能
 *
 * @author yvmoux
 * @since 1.0.0
 */
public final class VersionUtils {
    
    private VersionUtils() {
        // 工具类不允许实例化
    }

    /**
     * 获取服务器版本
     * @return String 服务器版本
     */
    @NotNull
    public static String getServerVersion() {
        return Bukkit.getVersion();
    }

    /**
     * 检查版本兼容性
     * @param minVersion 最小版本
     * @param maxVersion 最大版本
     * @return boolean 如果版本兼容返回true
     */
    public static boolean isVersionCompatible(@NotNull String minVersion, @NotNull String maxVersion) {
        String currentVersion = getServerVersion();
        return compareVersions(currentVersion, minVersion) >= 0 &&
               compareVersions(currentVersion, maxVersion) <= 0;
    }
    
    /**
     * 比较版本号
     * @param version1 版本1
     * @param version2 版本2
     * @return int 比较结果
     */
    private static int compareVersions(@NotNull String version1, @NotNull String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1Part < v2Part) {
                return -1;
            } else if (v1Part > v2Part) {
                return 1;
            }
        }
        
        return 0;
    }
    
    /**
     * 获取Minecraft版本
     * @return String Minecraft版本
     */
    @NotNull
    public static String getMinecraftVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }
    
    /**
     * 检查是否为1.19+版本
     * @return boolean 如果是1.19+版本返回true
     */
    public static boolean is1_19Plus() {
        return isVersionCompatible("1.19", "1.20");
    }
    
    /**
     * 检查是否为1.20+版本
     * @return boolean 如果是1.20+版本返回true
     */
    public static boolean is1_20Plus() {
        return isVersionCompatible("1.20", "1.21");
    }
} 