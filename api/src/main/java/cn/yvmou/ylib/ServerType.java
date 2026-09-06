package cn.yvmou.ylib;

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

    // Canvas 必须先于 FOLIA 探测：Canvas（Paper fork）同样含 RegionizedServer，
    // 若 FOLIA 先命中会把 Canvas 误判为 Folia
    CANVAS(
        "Canvas",
        "io.canvasmc.canvas.region.RegionThreadingTickManager"
    ),

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

    @Override
    public String toString() {
        if (this == ServerType.UNKNOWN) {
            return "Unknown Server Type";
        } if (this == ServerType.CANVAS) {
            return "Canvas";
        } if (this == ServerType.FOLIA) {
            return "Folia";
        } else if (this == ServerType.PAPER) {
            return "Paper";
        } else {
            return "Spigot";
        }
    }
}