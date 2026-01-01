package cn.yvmou.ylib.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 服务器类型枚举，用于自动检测和创建对应的YLib实现
 * 
 * @author yvmou
 * @since 1.0.0
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public enum ServerType {

    FOLIA(
        "Folia",
        "io.papermc.paper.threadedregions.RegionizedServer"
    ),

    PAPER(
        "Paper", 
        "com.destroystokyo.paper.PaperConfig"
    ),

    SPIGOT(
        "Spigot",
        "org.spigotmc.SpigotConfig"
    ),

    UNKNOWN(
        "Unknown",
        null
    );
    
    private final String displayName;
    private final String detectionClass;
    
    ServerType(@NotNull String displayName, @Nullable String detectionClass) {
        this.displayName = displayName;
        this.detectionClass = detectionClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDetectionClass() {
        return detectionClass;
    }


    @NotNull
    public static ServerType detectServerType() {
        for (ServerType type : values()) {
            if (type.detectionClass != null) {
                try {
                    Class.forName(type.detectionClass);
                    return type;
                } catch (ClassNotFoundException ignored) {
                }
            }

        }
        return UNKNOWN;
    }
}