package cn.yvmou.ylib.api.services;

import org.jetbrains.annotations.NotNull;

/**
 * 服务器信息服务
 *
 * @author admin
 * &#064;date  2025/08/08
 * @since 1.0.0-beta5
 */
public interface ServerInfoService {
    /**
     * 获取服务器类型
     *
     * @return 服务器类型
     */
    @NotNull
    String getServerType();

    /**
     * 检查是否为Folia服务器
     *
     * @return 如果是Folia服务器返回true，否则返回false
     */
    boolean isFolia();

    /**
     * 检查是否为Paper服务器（包括Folia）
     *
     * @return 如果是Paper或Folia服务器返回true，否则返回false
     */
    boolean isPaper();

    /**
     * 检查是否为Spigot服务器（包括Paper和Folia）
     *
     * @return 如果是Spigot、Paper或Folia服务器返回true，否则返回false
     */
    boolean isSpigot();

    /**
     * 获取服务器版本信息
     *
     * @return 服务器版本信息
     */
    @NotNull
    String getServerVersion();

    /**
     * 获取Bukkit版本信息
     *
     * @return Bukkit版本信息
     */
    @NotNull
    String getBukkitVersion();

    /**
     * 获取插件名称
     *
     * @return 插件名称
     */
    @NotNull
    String getPluginName();

    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    @NotNull
    String getPluginVersion();

    /**
     * 获取插件前缀
     *
     * @return 插件前缀
     */
    @NotNull
    String getPluginPrefix();

    /**
     * 获取YLib版本
     *
     * @return YLib版本
     */
    @NotNull
    String getYLibVersion();
}
