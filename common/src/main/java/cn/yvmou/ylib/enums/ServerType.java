package cn.yvmou.ylib.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 服务器类型枚举，用于自动检测和创建对应的YLib实现
 * 
 * @author yvmou
 * @since 1.0.0
 */
@SuppressWarnings("SpellCheckingInspection")
public enum ServerType {

    FOLIA(
        "Folia",
        "io.papermc.paper.threadedregions.RegionizedServer",
        1
    ),

    PAPER(
        "Paper", 
        "com.destroystokyo.paper.PaperConfig",
        2
    ),

    SPIGOT(
        "Spigot",
        "org.spigotmc.SpigotConfig",
        3
    ),

    UNKNOWN(
        "Unknown",
        null,
        999
    );
    
    private final String displayName;
    private final String detectionClass;
    private final int priority;
    
    ServerType(@NotNull String displayName, @Nullable String detectionClass, int priority) {
        this.displayName = displayName;
        this.detectionClass = detectionClass;
        this.priority = priority;
    }
    
    /**
     * 获取显示名称
     * 
     * @return 显示名称
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取检测类名
     * 
     * @return 检测类名，如果为UNKNOWN则返回null
     */
    @Nullable
    public String getDetectionClass() {
        return detectionClass;
    }

    /**
     * 获取优先级（数字越小优先级越高）
     * 
     * @return 优先级
     */
    public int getPriority() {
        return priority;
    }

    
    /**
     * 自动检测当前服务器类型
     * 
     * @return 检测到的服务器类型，如果无法检测则返回UNKNOWN
     */
    @NotNull
    public static ServerType detectServerType() {
        for (ServerType type : values()) {
            if (type == UNKNOWN) continue;
            
            if (type.selfCheck()) {
                return type;
            }
        }

        return UNKNOWN;
    }

    /*
       ┌─────────────────────────────────────────────────────────────────┐
       │  私有方法 | Private Method
       └─────────────────────────────────────────────────────────────────┘
     */

    private boolean selfCheck() {
        if (detectionClass == null) {
            return false;
        }

        try {
            Class.forName(detectionClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}