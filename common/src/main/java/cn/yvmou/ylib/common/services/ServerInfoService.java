package cn.yvmou.ylib.common.services;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 服务器信息提供者
 * 处理所有与服务器类型、版本等信息相关的功能
 */
public class ServerInfoService implements cn.yvmou.ylib.api.services.ServerInfoService {
    private final JavaPlugin plugin;

    public ServerInfoService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public String getServerType() {
        if (isFolia()) {
            return "FOLIA";
        } else if (isPaper()) {
            return "PAPER";
        } else if (isSpigot()) {
            return "SPIGOT";
        } else {
            return "UNKNOWN";
        }
    }

    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    public String getServerVersion() {
        try {
            return Bukkit.getVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @NotNull
    public String getBukkitVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @NotNull
    public String getPluginName() {
        return plugin.getName();
    }

    @NotNull
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @NotNull
    public String getPluginPrefix() {
        if (plugin.getDescription().getPrefix() != null) {
            return plugin.getDescription().getPrefix();
        }
        return "[" + getPluginName() + "]";
    }

    @NotNull
    public String getYLibVersion() {
        return "1.0.0-beta5";
    }
}
