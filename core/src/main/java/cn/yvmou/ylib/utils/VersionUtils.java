package cn.yvmou.ylib.utils;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
     * @return String 服务器版本，如 "1.19.4"
     */
    @NotNull
    public static String getServerVersion() {
        return getMinecraftVersion();
    }

    /**
     * 检查版本是否在 [minVersion, maxVersion] 闭区间内
     * @param minVersion 最小版本
     * @param maxVersion 最大版本
     * @return boolean 如果版本兼容返回true
     */
    public static boolean isVersionCompatible(@NotNull String minVersion, @NotNull String maxVersion) {
        return compareVersions(getServerVersion(), minVersion) >= 0 &&
               compareVersions(getServerVersion(), maxVersion) <= 0;
    }

    /**
     * 比较版本号（只比较数字段，忽略前缀文本和 -R0.1 之类后缀）
     * @param version1 版本1
     * @param version2 版本2
     * @return int 比较结果
     */
    private static int compareVersions(@NotNull String version1, @NotNull String version2) {
        int[] v1Parts = toVersionParts(version1);
        int[] v2Parts = toVersionParts(version2);

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? v1Parts[i] : 0;
            int v2Part = i < v2Parts.length ? v2Parts[i] : 0;

            if (v1Part != v2Part) {
                return Integer.compare(v1Part, v2Part);
            }
        }

        return 0;
    }

    /**
     * 将版本字符串解析为数字段数组。
     * 例如 "git-Paper-318 (MC: 1.19.4)" -> [1, 19, 4]
     */
    private static int[] toVersionParts(@NotNull String version) {
        String[] raw = version.split("[^0-9]+");
        List<Integer> parts = new ArrayList<>();
        for (String part : raw) {
            if (!part.isEmpty()) {
                parts.add(Integer.parseInt(part));
            }
        }
        int[] result = new int[parts.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = parts.get(i);
        }
        return result;
    }

    /**
     * 获取Minecraft版本
     * @return String Minecraft版本，如 "1.19.4"
     */
    @NotNull
    public static String getMinecraftVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /**
     * 检查是否为1.19+版本
     * @return boolean 如果是1.19及以上版本返回true
     */
    public static boolean is1_19Plus() {
        return compareVersions(getServerVersion(), "1.19") >= 0;
    }

    /**
     * 检查是否为1.20+版本
     * @return boolean 如果是1.20及以上版本返回true
     */
    public static boolean is1_20Plus() {
        return compareVersions(getServerVersion(), "1.20") >= 0;
    }
}
